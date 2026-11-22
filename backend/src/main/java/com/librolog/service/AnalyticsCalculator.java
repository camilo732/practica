package com.librolog.service;

import com.librolog.dto.response.AuthorCountResponse;
import com.librolog.dto.response.GenreCountResponse;
import com.librolog.dto.response.MonthlyActivityResponse;
import com.librolog.dto.response.RatingCountResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cálculos puros de las estadísticas de lectura.
 *
 * <p>No conoce Spring ni la base de datos: recibe listas ya consultadas y
 * devuelve los agregados. Así se puede probar con fechas y datos fijos.</p>
 */
public final class AnalyticsCalculator {

    private AnalyticsCalculator() {
    }

    /**
     * Libro terminado, con la fecha y la autoría.
     *
     * @param finishedAt fecha de fin
     * @param author     autoría, puede ser nula
     */
    public record FinishedBook(LocalDate finishedAt, String author) {
    }

    /**
     * Páginas leídas en un día.
     *
     * @param date  día del registro
     * @param pages páginas leídas
     */
    public record PageLog(LocalDate date, int pages) {
    }

    /**
     * Construye la serie de los doce meses de un año.
     *
     * @param year     año
     * @param finished libros terminados
     * @param logs     registros de páginas
     * @return actividad de enero a diciembre, siempre doce elementos
     */
    public static List<MonthlyActivityResponse> monthly(int year,
                                                        List<FinishedBook> finished,
                                                        List<PageLog> logs) {
        long[] books = new long[12];
        long[] pages = new long[12];

        for (FinishedBook book : finished) {
            if (book.finishedAt() != null && book.finishedAt().getYear() == year) {
                books[book.finishedAt().getMonthValue() - 1]++;
            }
        }
        for (PageLog log : logs) {
            if (log.date() != null && log.date().getYear() == year && log.pages() > 0) {
                pages[log.date().getMonthValue() - 1] += log.pages();
            }
        }

        List<MonthlyActivityResponse> result = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            result.add(new MonthlyActivityResponse(month, books[month - 1], pages[month - 1]));
        }
        return result;
    }

    /**
     * Calcula la calificación media.
     *
     * <p>Solo se tienen en cuenta las calificaciones entre 1 y 5. El resultado
     * se redondea a un decimal y vale 0 si no hay reseñas.</p>
     *
     * @param ratings calificaciones de las reseñas
     * @return media con un decimal
     */
    public static double averageRating(List<Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return 0.0;
        }

        long sum = 0;
        int count = 0;
        for (Integer rating : ratings) {
            if (rating != null && rating >= 1 && rating <= 5) {
                sum += rating;
                count++;
            }
        }
        if (count == 0) {
            return 0.0;
        }
        return Math.round(sum * 10.0 / count) / 10.0;
    }

    /**
     * Cuenta las reseñas de cada calificación.
     *
     * <p>Siempre devuelve las cinco calificaciones, aunque alguna esté a cero,
     * para que el gráfico mantenga todas sus barras.</p>
     *
     * @param ratings calificaciones de las reseñas
     * @return conteo de 1 a 5 estrellas
     */
    public static List<RatingCountResponse> ratingDistribution(List<Integer> ratings) {
        long[] counts = new long[5];
        if (ratings != null) {
            for (Integer rating : ratings) {
                if (rating != null && rating >= 1 && rating <= 5) {
                    counts[rating - 1]++;
                }
            }
        }

        List<RatingCountResponse> result = new ArrayList<>(5);
        for (int rating = 1; rating <= 5; rating++) {
            result.add(new RatingCountResponse(rating, counts[rating - 1]));
        }
        return result;
    }

    /**
     * Obtiene las autorías con más libros terminados.
     *
     * <p>La comparación no distingue mayúsculas de minúsculas y se conserva la
     * primera grafía encontrada. Las autorías vacías se ignoran.</p>
     *
     * @param finished libros terminados
     * @param limit    número máximo de autorías
     * @return autorías ordenadas de más a menos libros
     */
    public static List<AuthorCountResponse> topAuthors(List<FinishedBook> finished, int limit) {
        if (finished == null || limit <= 0) {
            return List.of();
        }

        Map<String, AuthorCountResponse> counts =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (FinishedBook book : finished) {
            String author = book.author() == null ? "" : book.author().trim();
            if (author.isEmpty()) {
                continue;
            }
            AuthorCountResponse current = counts.get(author);
            counts.put(author, current == null
                    ? new AuthorCountResponse(author, 1)
                    : new AuthorCountResponse(current.author(), current.books() + 1));
        }

        return counts.values().stream()
                .sorted(Comparator.comparingLong(AuthorCountResponse::books).reversed()
                        .thenComparing(AuthorCountResponse::author,
                                String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .toList();
    }

    /**
     * Encuentra el mes con más páginas leídas.
     *
     * @param months actividad mensual
     * @return mes de 1 a 12, o 0 si no hay páginas
     */
    public static int busiestMonth(List<MonthlyActivityResponse> months) {
        int bestMonth = 0;
        long bestPages = 0;

        if (months != null) {
            for (MonthlyActivityResponse month : months) {
                if (month.pagesRead() > bestPages) {
                    bestPages = month.pagesRead();
                    bestMonth = month.month();
                }
            }
        }
        return bestMonth;
    }

    /**
     * Obtiene los géneros con más libros terminados.
     *
     * <p>Igual que las autorías: no distingue mayúsculas, conserva la primera
     * grafía e ignora los géneros vacíos.</p>
     *
     * @param genres género de cada libro terminado (puede contener nulos)
     * @param limit  número máximo de géneros
     * @return géneros ordenados de más a menos libros
     */
    public static List<GenreCountResponse> topGenres(List<String> genres, int limit) {
        if (genres == null || limit <= 0) {
            return List.of();
        }

        Map<String, GenreCountResponse> counts =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (String raw : genres) {
            String genre = raw == null ? "" : raw.trim();
            if (genre.isEmpty()) {
                continue;
            }
            GenreCountResponse current = counts.get(genre);
            counts.put(genre, current == null
                    ? new GenreCountResponse(genre, 1)
                    : new GenreCountResponse(current.genre(), current.books() + 1));
        }

        return counts.values().stream()
                .sorted(Comparator.comparingLong(GenreCountResponse::books).reversed()
                        .thenComparing(GenreCountResponse::genre,
                                String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .toList();
    }

    /**
     * Calcula el promedio de páginas por libro.
     *
     * <p>Solo cuenta los libros con un número de páginas mayor que cero. El
     * resultado se redondea a un decimal y vale 0 si no hay libros.</p>
     *
     * @param pages páginas de cada libro
     * @return promedio con un decimal
     */
    public static double averagePages(List<Integer> pages) {
        if (pages == null || pages.isEmpty()) {
            return 0.0;
        }
        long sum = 0;
        int count = 0;
        for (Integer value : pages) {
            if (value != null && value > 0) {
                sum += value;
                count++;
            }
        }
        if (count == 0) {
            return 0.0;
        }
        return Math.round(sum * 10.0 / count) / 10.0;
    }
}
