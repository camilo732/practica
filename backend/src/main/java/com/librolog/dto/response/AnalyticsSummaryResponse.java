package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resumen estadístico de la actividad lectora de un usuario.
 *
 * <p>Los datos se calculan en el momento de la consulta. El año solo afecta a
 * las métricas del periodo; los totales de la biblioteca y las páginas
 * históricas abarcan todo el historial.</p>
 *
 * @param year               año del periodo consultado
 * @param totalBooks         libros en la biblioteca
 * @param wantToRead         libros pendientes
 * @param reading            libros en lectura
 * @param read               libros terminados
 * @param finishedThisYear   libros terminados en el año
 * @param finishedThisMonth  libros terminados en el mes actual; 0 si se consulta otro año
 * @param pagesReadTotal     páginas leídas en total
 * @param pagesReadThisYear  páginas leídas en el año
 * @param pagesReadThisMonth páginas leídas en el mes actual; 0 si se consulta otro año
 * @param averageRating      calificación media, con un decimal
 * @param reviewsCount       número de reseñas
 * @param busiestMonth       mes con más páginas, 0 si no hay actividad
 * @param monthlyActivity    actividad de los doce meses
 * @param ratingDistribution reseñas por calificación
 * @param topAuthors         autorías con más libros terminados en el año
 * @param topGenres          géneros con más libros terminados en el año
 * @param averagePagesPerBook promedio de páginas de los libros terminados en el año
 */
@Schema(description = "Resumen estadístico de lectura")
public record AnalyticsSummaryResponse(

        @Schema(description = "Año consultado", example = "2026")
        int year,

        @Schema(description = "Libros en la biblioteca", example = "42")
        int totalBooks,

        @Schema(description = "Pendientes", example = "10")
        int wantToRead,

        @Schema(description = "En lectura", example = "3")
        int reading,

        @Schema(description = "Terminados", example = "29")
        int read,

        @Schema(description = "Terminados en el año", example = "15")
        int finishedThisYear,

        @Schema(description = "Terminados este mes", example = "2")
        int finishedThisMonth,

        @Schema(description = "Páginas leídas en total", example = "12500")
        long pagesReadTotal,

        @Schema(description = "Páginas leídas en el año", example = "5100")
        long pagesReadThisYear,

        @Schema(description = "Páginas leídas este mes", example = "640")
        long pagesReadThisMonth,

        @Schema(description = "Calificación media", example = "4.3")
        double averageRating,

        @Schema(description = "Número de reseñas", example = "12")
        int reviewsCount,

        @Schema(description = "Mes con más páginas (0 si no hay datos)", example = "11")
        int busiestMonth,

        @Schema(description = "Actividad por mes")
        List<MonthlyActivityResponse> monthlyActivity,

        @Schema(description = "Distribución de calificaciones")
        List<RatingCountResponse> ratingDistribution,

        @Schema(description = "Autorías más leídas en el año")
        List<AuthorCountResponse> topAuthors,

        @Schema(description = "Géneros más leídos en el año")
        List<GenreCountResponse> topGenres,

        @Schema(description = "Promedio de páginas por libro terminado en el año", example = "312.5")
        double averagePagesPerBook) {

    /**
     * Constructor compatible con la versión anterior (sin géneros ni promedio
     * de páginas). Lo usan las pruebas existentes del informe PDF.
     */
    public AnalyticsSummaryResponse(int year, int totalBooks, int wantToRead, int reading,
                                    int read, int finishedThisYear, int finishedThisMonth,
                                    long pagesReadTotal, long pagesReadThisYear,
                                    long pagesReadThisMonth, double averageRating,
                                    int reviewsCount, int busiestMonth,
                                    List<MonthlyActivityResponse> monthlyActivity,
                                    List<RatingCountResponse> ratingDistribution,
                                    List<AuthorCountResponse> topAuthors) {
        this(year, totalBooks, wantToRead, reading, read, finishedThisYear, finishedThisMonth,
                pagesReadTotal, pagesReadThisYear, pagesReadThisMonth, averageRating,
                reviewsCount, busiestMonth, monthlyActivity, ratingDistribution, topAuthors,
                List.of(), 0.0);
    }
}
