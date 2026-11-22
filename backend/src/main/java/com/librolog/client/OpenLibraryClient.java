package com.librolog.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente de la API pública de Open Library.
 *
 * <p>Solo se usan los campos imprescindibles del buscador para que la respuesta
 * sea pequeña y rápida. Los fallos de red se propagan como excepciones de
 * {@link RestClient}; la capa de servicio decide si degrada a la caché local.</p>
 */
@Component
public class OpenLibraryClient {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryClient.class);

    /** Campos solicitados al buscador. */
    private static final String FIELDS =
            "key,title,author_name,first_publish_year,cover_i,number_of_pages_median,isbn,subject";

    /**
     * Temas de Open Library que no describen un género (formato, préstamo,
     * accesibilidad). Se ignoran al elegir el género del libro.
     */
    private static final List<String> NON_GENRE_SUBJECTS = List.of(
            "accessible book", "protected daisy", "in library", "lending library",
            "large type books", "open library staff picks", "overdrive", "nyt:",
            "new york times", "reading level", "translations into", "long now");

    /** Plantilla de las portadas; el sufijo {@code M} pide tamaño medio. */
    private static final String COVER_URL = "https://covers.openlibrary.org/b/id/%d-M.jpg";

    /** Clave válida de una obra. Evita construir rutas arbitrarias. */
    private static final Pattern WORK_KEY = Pattern.compile("^/works/OL[A-Za-z0-9]+W$");

    private static final Pattern YEAR = Pattern.compile("\\b(\\d{4})\\b");

    private final RestClient restClient;

    /**
     * Crea el cliente con el {@link RestClient} configurado.
     *
     * @param openLibraryRestClient cliente HTTP con la URL base de Open Library
     */
    public OpenLibraryClient(RestClient openLibraryRestClient) {
        this.restClient = openLibraryRestClient;
    }

    /**
     * Busca obras por título, autoría o ISBN.
     *
     * @param query texto de búsqueda
     * @param limit número máximo de resultados
     * @return obras encontradas; lista vacía si no hay coincidencias
     */
    public List<OpenLibraryBook> search(String query, int limit) {
        Map<String, Object> body = restClient.get()
                .uri(builder -> builder.path("/search.json")
                        .queryParam("q", query)
                        .queryParam("limit", limit)
                        .queryParam("fields", FIELDS)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        if (body == null || !(body.get("docs") instanceof List<?> docs)) {
            return List.of();
        }

        List<OpenLibraryBook> books = new ArrayList<>();
        for (Object raw : docs) {
            if (raw instanceof Map<?, ?> doc) {
                @SuppressWarnings("unchecked")
                OpenLibraryBook book = mapDoc((Map<String, Object>) doc);
                if (book != null) {
                    books.add(book);
                }
            }
        }
        log.debug("Open Library devolvió {} resultados para '{}'", books.size(), query);
        return books;
    }

    /**
     * Consulta el detalle de una obra concreta.
     *
     * @param openLibraryKey clave de la obra
     * @return detalle de la obra, o vacío si no existe o la clave no es válida
     */
    public Optional<OpenLibraryBook> work(String openLibraryKey) {
        if (!isValidKey(openLibraryKey)) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = restClient.get()
                    .uri(openLibraryKey + ".json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (body == null) {
                return Optional.empty();
            }
            return Optional.of(mapWork(openLibraryKey, body));
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("Open Library no conoce la obra {}", openLibraryKey);
            return Optional.empty();
        }
    }

    /**
     * Comprueba que una clave tiene el formato de una obra de Open Library.
     *
     * @param key clave recibida
     * @return verdadero si es segura para construir una ruta
     */
    public static boolean isValidKey(String key) {
        return key != null && WORK_KEY.matcher(key).matches();
    }

    /**
     * Convierte un documento del buscador en un libro.
     *
     * <p>Es estático y sin dependencias para poder probarlo con mapas de
     * ejemplo, sin llamar a la red.</p>
     *
     * @param doc documento JSON del buscador
     * @return libro, o {@code null} si no tiene título
     */
    static OpenLibraryBook mapDoc(Map<String, Object> doc) {
        String title = text(doc.get("title"));
        if (title == null) {
            return null;
        }

        String key = text(doc.get("key"));
        Integer coverId = number(doc.get("cover_i"));

        return new OpenLibraryBook(
                key,
                truncate(title, 500),
                truncate(firstText(doc.get("author_name")), 500),
                coverId == null ? null : COVER_URL.formatted(coverId),
                number(doc.get("number_of_pages_median")),
                number(doc.get("first_publish_year")),
                truncate(firstText(doc.get("isbn")), 20),
                null,
                genre(doc.get("subject")));
    }

    private static OpenLibraryBook mapWork(String key, Map<String, Object> doc) {
        String title = text(doc.get("title"));
        if (title == null) {
            title = "Sin título";
        }

        Integer coverId = null;
        if (doc.get("covers") instanceof List<?> covers && !covers.isEmpty()) {
            coverId = number(covers.get(0));
        }

        Integer year = null;
        String publishDate = text(doc.get("first_publish_date"));
        if (publishDate != null) {
            Matcher matcher = YEAR.matcher(publishDate);
            if (matcher.find()) {
                year = Integer.valueOf(matcher.group(1));
            }
        }

        return new OpenLibraryBook(
                key,
                truncate(title, 500),
                null,
                coverId == null ? null : COVER_URL.formatted(coverId),
                null,
                year,
                null,
                truncate(description(doc.get("description")), 4000),
                genre(doc.get("subjects")));
    }

    /**
     * Elige el primer tema que parece un género real.
     *
     * @param raw lista de temas de Open Library
     * @return género con mayúscula inicial, o {@code null} si no hay ninguno útil
     */
    static String genre(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return null;
        }
        for (Object item : list) {
            String value = text(item);
            if (value == null || value.length() > 60) {
                continue;
            }
            String lower = value.toLowerCase(Locale.ROOT);
            boolean noise = NON_GENRE_SUBJECTS.stream().anyMatch(lower::contains);
            if (!noise) {
                return truncate(Character.toUpperCase(value.charAt(0)) + value.substring(1), 100);
            }
        }
        return null;
    }

    private static String description(Object raw) {
        if (raw instanceof String value) {
            return value.isBlank() ? null : value.trim();
        }
        if (raw instanceof Map<?, ?> map) {
            return text(map.get("value"));
        }
        return null;
    }

    private static String text(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private static String firstText(Object raw) {
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                String value = text(item);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }
        return text(raw);
    }

    private static Integer number(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String value) {
            try {
                return Integer.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
