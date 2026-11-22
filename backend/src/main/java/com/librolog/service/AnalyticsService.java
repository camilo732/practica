package com.librolog.service;

import com.librolog.dto.response.AnalyticsMonthlyResponse;
import com.librolog.dto.response.AnalyticsSummaryResponse;
import com.librolog.dto.response.MonthlyActivityResponse;
import com.librolog.entity.ReadingLog;
import com.librolog.entity.Review;
import com.librolog.entity.UserBook;
import com.librolog.repository.ReadingLogRepository;
import com.librolog.repository.ReviewRepository;
import com.librolog.repository.UserBookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Estadísticas de lectura de un usuario.
 *
 * <p>Combina la biblioteca, las reseñas y la bitácora de páginas. Los cálculos
 * puros viven en {@link AnalyticsCalculator}; este servicio solo consulta y
 * prepara los datos.</p>
 */
@Service
public class AnalyticsService {

    /** Primer año admitido en las consultas. */
    private static final int MIN_YEAR = 2000;

    /** Último año admitido en las consultas. */
    private static final int MAX_YEAR = 2100;

    /** Número de autorías que se devuelven en el resumen. */
    private static final int TOP_AUTHORS = 5;

    /** Número de géneros que se devuelven en el resumen. */
    private static final int TOP_GENRES = 5;

    private final UserBookRepository userBookRepository;
    private final ReviewRepository reviewRepository;
    private final ReadingLogRepository readingLogRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userBookRepository   repositorio de la biblioteca
     * @param reviewRepository     repositorio de reseñas
     * @param readingLogRepository repositorio de la bitácora
     */
    public AnalyticsService(UserBookRepository userBookRepository,
                            ReviewRepository reviewRepository,
                            ReadingLogRepository readingLogRepository) {
        this.userBookRepository = userBookRepository;
        this.reviewRepository = reviewRepository;
        this.readingLogRepository = readingLogRepository;
    }

    /**
     * Calcula el resumen estadístico del año indicado.
     *
     * @param userId usuario propietario
     * @param year   año, o {@code null} para el actual
     * @return resumen con totales, actividad mensual y distribuciones
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(Long userId, Integer year) {
        int selectedYear = resolveYear(year);
        LocalDate today = LocalDate.now();

        List<UserBook> entries = userBookRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Review> reviews = reviewRepository.findByUserBookUserId(userId);

        LocalDate start = LocalDate.of(selectedYear, 1, 1);
        LocalDate end = LocalDate.of(selectedYear, 12, 31);
        List<ReadingLog> logs = readingLogRepository
                .findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, start, end);

        List<AnalyticsCalculator.FinishedBook> finished = new ArrayList<>();
        List<String> genresThisYear = new ArrayList<>();
        List<Integer> pagesThisYearBooks = new ArrayList<>();
        int wantToRead = 0;
        int reading = 0;
        int read = 0;

        for (UserBook entry : entries) {
            switch (entry.getStatus()) {
                case WANT_TO_READ -> wantToRead++;
                case READING -> reading++;
                case READ -> read++;
            }
            if (entry.getFinishedAt() != null) {
                finished.add(new AnalyticsCalculator.FinishedBook(
                        entry.getFinishedAt(),
                        entry.getBook() == null ? null : entry.getBook().getAuthor()));
                if (entry.getFinishedAt().getYear() == selectedYear) {
                    genresThisYear.add(entry.getBook() == null ? null : entry.getBook().getGenre());
                    pagesThisYearBooks.add(entry.getTotalPages() > 0
                            ? Integer.valueOf(entry.getTotalPages())
                            : (entry.getBook() == null ? null : entry.getBook().getTotalPages()));
                }
            }
        }

        List<AnalyticsCalculator.PageLog> pageLogs = logs.stream()
                .map(log -> new AnalyticsCalculator.PageLog(
                        log.getLogDate(),
                        log.getPagesRead() == null ? 0 : log.getPagesRead()))
                .toList();

        List<MonthlyActivityResponse> monthly =
                AnalyticsCalculator.monthly(selectedYear, finished, pageLogs);

        List<AnalyticsCalculator.FinishedBook> finishedThisYear = finished.stream()
                .filter(book -> book.finishedAt() != null
                        && book.finishedAt().getYear() == selectedYear)
                .toList();

        boolean currentYear = selectedYear == today.getYear();

        int finishedThisMonth = currentYear
                ? (int) finishedThisYear.stream()
                        .filter(book -> book.finishedAt().getMonthValue() == today.getMonthValue())
                        .count()
                : 0;

        long pagesThisYear = pageLogs.stream()
                .filter(log -> log.date() != null && log.date().getYear() == selectedYear)
                .mapToLong(AnalyticsCalculator.PageLog::pages)
                .sum();

        long pagesThisMonth = currentYear
                ? pageLogs.stream()
                        .filter(log -> log.date() != null
                                && log.date().getMonthValue() == today.getMonthValue())
                        .mapToLong(AnalyticsCalculator.PageLog::pages)
                        .sum()
                : 0;

        List<Integer> ratings = reviews.stream()
                .map(Review::getRating)
                .filter(rating -> rating != null)
                .map(Short::intValue)
                .toList();

        return new AnalyticsSummaryResponse(
                selectedYear,
                entries.size(),
                wantToRead,
                reading,
                read,
                finishedThisYear.size(),
                finishedThisMonth,
                readingLogRepository.sumPagesByUser(userId),
                pagesThisYear,
                pagesThisMonth,
                AnalyticsCalculator.averageRating(ratings),
                reviews.size(),
                AnalyticsCalculator.busiestMonth(monthly),
                monthly,
                AnalyticsCalculator.ratingDistribution(ratings),
                AnalyticsCalculator.topAuthors(finishedThisYear, TOP_AUTHORS),
                AnalyticsCalculator.topGenres(genresThisYear, TOP_GENRES),
                AnalyticsCalculator.averagePages(pagesThisYearBooks));
    }

    /**
     * Devuelve solo la serie mensual del año indicado.
     *
     * @param userId usuario propietario
     * @param year   año, o {@code null} para el actual
     * @return serie de doce meses
     */
    @Transactional(readOnly = true)
    public AnalyticsMonthlyResponse monthly(Long userId, Integer year) {
        AnalyticsSummaryResponse summary = summary(userId, year);
        return new AnalyticsMonthlyResponse(summary.year(), summary.monthlyActivity());
    }

    /**
     * Valida el año y aplica el actual cuando no llega.
     *
     * @param year año solicitado
     * @return año válido
     * @throws IllegalArgumentException si el año está fuera de rango
     */
    public static int resolveYear(Integer year) {
        if (year == null) {
            return LocalDate.now().getYear();
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException(
                    "El año debe estar entre " + MIN_YEAR + " y " + MAX_YEAR + ".");
        }
        return year;
    }
}
