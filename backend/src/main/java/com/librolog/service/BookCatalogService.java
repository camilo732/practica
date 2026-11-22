package com.librolog.service;

import com.librolog.client.OpenLibraryBook;
import com.librolog.client.OpenLibraryClient;
import com.librolog.dto.request.AddUserBookRequest;
import com.librolog.dto.response.BookResponse;
import com.librolog.dto.response.BookSearchResponse;
import com.librolog.entity.Book;
import com.librolog.exception.ServiceUnavailableException;
import com.librolog.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Catálogo de libros con caché local de Open Library.
 *
 * <p>La búsqueda consulta primero la API pública y guarda el resultado en la
 * tabla {@code books}. Si Open Library no responde, se devuelve lo que ya hay
 * en la caché; solo si tampoco hay nada se responde 503. La llamada remota se
 * hace fuera de la transacción para no retener una conexión durante la espera
 * de red.</p>
 */
@Service
public class BookCatalogService {

    private static final Logger log = LoggerFactory.getLogger(BookCatalogService.class);

    /** Mínimo de caracteres para lanzar una búsqueda. */
    private static final int MIN_QUERY_LENGTH = 2;

    /** Máximo de resultados por búsqueda. */
    private static final int MAX_LIMIT = 40;

    private final BookRepository bookRepository;
    private final OpenLibraryClient openLibraryClient;
    private final TransactionTemplate transactionTemplate;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param bookRepository     repositorio del catálogo
     * @param openLibraryClient  cliente de Open Library
     * @param transactionManager gestor de transacciones
     */
    public BookCatalogService(BookRepository bookRepository,
                              OpenLibraryClient openLibraryClient,
                              PlatformTransactionManager transactionManager) {
        this.bookRepository = bookRepository;
        this.openLibraryClient = openLibraryClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Busca libros en Open Library y actualiza la caché local.
     *
     * @param rawQuery texto introducido por la persona usuaria
     * @param limit    número máximo de resultados
     * @return resultados y origen de los mismos
     * @throws IllegalArgumentException si la consulta es demasiado corta
     * @throws ServiceUnavailableException si no hay resultados locales ni remotos
     */
    public BookSearchResponse search(String rawQuery, int limit) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < MIN_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Escribe al menos " + MIN_QUERY_LENGTH + " caracteres para buscar.");
        }

        int safeLimit = Math.max(1, Math.min(MAX_LIMIT, limit));

        try {
            List<OpenLibraryBook> remote = openLibraryClient.search(query, safeLimit);
            if (!remote.isEmpty()) {
                List<BookResponse> items = transactionTemplate.execute(
                        status -> saveAll(remote, safeLimit));
                if (items != null && !items.isEmpty()) {
                    return new BookSearchResponse(items, items.size(), false);
                }
            }
        } catch (RestClientException ex) {
            log.warn("Open Library no está disponible para '{}': {}", query, ex.getMessage());
        }

        List<Book> cached = bookRepository.search(query, PageRequest.of(0, safeLimit));
        if (cached.isEmpty()) {
            throw new ServiceUnavailableException(
                    "El catálogo externo no está disponible y no hay resultados guardados "
                            + "para esa búsqueda. Inténtalo de nuevo en unos minutos.");
        }

        List<BookResponse> items = cached.stream().map(BookResponse::from).toList();
        return new BookSearchResponse(items, items.size(), true);
    }

    /**
     * Devuelve un libro de la caché.
     *
     * @param id identificador del libro
     * @return datos públicos del libro
     * @throws NoSuchElementException si no existe
     */
    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return bookRepository.findById(id)
                .map(BookResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No existe el libro indicado."));
    }

    /**
     * Obtiene el libro que se va a añadir a la biblioteca.
     *
     * <p>Prioriza un libro ya cacheado. Si solo llega la clave de Open Library
     * se consulta la caché y, en último término, el detalle de la obra. Si no
     * hay clave se crea un libro manual con los datos recibidos.</p>
     *
     * @param request datos de alta
     * @return entidad gestionada dentro de la transacción actual
     * @throws NoSuchElementException   si no se encuentra el libro ni se puede crear
     * @throws IllegalArgumentException si faltan datos o la clave no es válida
     */
    @Transactional
    public Book resolveBook(AddUserBookRequest request) {
        if (request.bookId() != null) {
            return bookRepository.findById(request.bookId())
                    .orElseThrow(() -> new NoSuchElementException("No existe el libro indicado."));
        }

        String key = request.openLibraryKey();
        if (key != null && !key.isBlank()) {
            if (!OpenLibraryClient.isValidKey(key)) {
                throw new IllegalArgumentException("La clave de Open Library no tiene un formato válido.");
            }

            Book cached = bookRepository.findByOpenLibraryKey(key).orElse(null);
            if (cached != null) {
                applyManualData(cached, request);
                return cached;
            }

            OpenLibraryBook detail = fetchDetail(key);
            if (detail == null && isBlank(request.title())) {
                throw new NoSuchElementException(
                        "No se encontró el libro en Open Library. Indica el título para crearlo manualmente.");
            }
            return bookRepository.save(buildFromKey(key, detail, request));
        }

        if (!isBlank(request.title())) {
            return bookRepository.save(buildManual(request));
        }

        throw new IllegalArgumentException(
                "Indica un bookId, una openLibraryKey o un título para añadir el libro.");
    }

    private OpenLibraryBook fetchDetail(String key) {
        try {
            return openLibraryClient.work(key).orElse(null);
        } catch (RestClientException ex) {
            log.warn("No se pudo consultar la obra {}: {}", key, ex.getMessage());
            return null;
        }
    }

    private List<BookResponse> saveAll(List<OpenLibraryBook> remote, int limit) {
        List<BookResponse> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (OpenLibraryBook item : remote) {
            if (item.openLibraryKey() != null && !seen.add(item.openLibraryKey())) {
                continue;
            }
            Book book = upsert(item);
            items.add(BookResponse.from(book));
            if (items.size() >= limit) {
                break;
            }
        }
        return items;
    }

    private Book upsert(OpenLibraryBook remote) {
        Book book = remote.openLibraryKey() == null
                ? null
                : bookRepository.findByOpenLibraryKey(remote.openLibraryKey()).orElse(null);

        if (book == null) {
            book = new Book();
            book.setOpenLibraryKey(remote.openLibraryKey());
            book.setTitle(remote.title() == null ? "Sin título" : remote.title());
        }

        if (remote.title() != null) {
            book.setTitle(remote.title());
        }
        if (remote.author() != null) {
            book.setAuthor(remote.author());
        }
        if (remote.coverUrl() != null) {
            book.setCoverUrl(remote.coverUrl());
        }
        if (remote.totalPages() != null && remote.totalPages() > 0) {
            book.setTotalPages(remote.totalPages());
        }
        if (remote.publishYear() != null && remote.publishYear() >= 1000) {
            book.setPublishYear(remote.publishYear());
        }
        if (remote.isbn() != null) {
            book.setIsbn(remote.isbn());
        }
        if (remote.description() != null) {
            book.setDescription(remote.description());
        }
        if (remote.genre() != null) {
            book.setGenre(remote.genre());
        }
        return bookRepository.save(book);
    }

    private Book buildFromKey(String key, OpenLibraryBook detail, AddUserBookRequest request) {
        Book book = new Book();
        book.setOpenLibraryKey(key);
        book.setTitle(firstNonBlank(
                request.title(),
                detail == null ? null : detail.title(),
                "Sin título"));
        book.setAuthor(firstNonBlank(
                request.author(),
                detail == null ? null : detail.author()));
        book.setCoverUrl(firstNonBlank(
                request.coverUrl(),
                detail == null ? null : detail.coverUrl()));
        book.setIsbn(firstNonBlank(
                request.isbn(),
                detail == null ? null : detail.isbn()));

        Integer pages = request.totalPages() != null
                ? request.totalPages()
                : (detail == null ? null : detail.totalPages());
        book.setTotalPages(pages);

        Integer year = request.publishYear() != null
                ? request.publishYear()
                : (detail == null ? null : detail.publishYear());
        book.setPublishYear(year);

        if (detail != null) {
            book.setDescription(detail.description());
            book.setGenre(detail.genre());
        }
        return book;
    }

    private Book buildManual(AddUserBookRequest request) {
        Book book = new Book();
        book.setTitle(request.title().trim());
        book.setAuthor(trimToNull(request.author()));
        book.setCoverUrl(trimToNull(request.coverUrl()));
        book.setTotalPages(request.totalPages());
        book.setPublishYear(request.publishYear());
        book.setIsbn(trimToNull(request.isbn()));
        return book;
    }

    private void applyManualData(Book book, AddUserBookRequest request) {
        boolean changed = false;
        if (book.getTotalPages() == null && request.totalPages() != null) {
            book.setTotalPages(request.totalPages());
            changed = true;
        }
        if (book.getAuthor() == null && !isBlank(request.author())) {
            book.setAuthor(request.author().trim());
            changed = true;
        }
        if (changed) {
            bookRepository.save(book);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
