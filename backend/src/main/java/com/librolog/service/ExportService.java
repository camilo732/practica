package com.librolog.service;

import com.librolog.dto.response.BookResponse;
import com.librolog.dto.response.LibraryExportResponse;
import com.librolog.dto.response.UserBookResponse;
import com.librolog.entity.ReadingGoal;
import com.librolog.entity.Review;
import com.librolog.entity.UserBook;
import com.librolog.repository.ReadingGoalRepository;
import com.librolog.repository.ReviewRepository;
import com.librolog.repository.UserBookRepository;
import com.librolog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exportación de la biblioteca personal.
 *
 * <p>Ofrece dos formatos: CSV, pensado para hojas de cálculo y para volver a
 * importar en LibroLog, y JSON, pensado como copia de seguridad completa porque
 * incluye reseñas y metas.</p>
 */
@Service
public class ExportService {

    private final UserBookRepository userBookRepository;
    private final ReviewRepository reviewRepository;
    private final ReadingGoalRepository goalRepository;
    private final UserRepository userRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userBookRepository repositorio de la biblioteca
     * @param reviewRepository   repositorio de reseñas
     * @param goalRepository     repositorio de metas
     * @param userRepository     repositorio de usuarios
     */
    public ExportService(UserBookRepository userBookRepository,
                         ReviewRepository reviewRepository,
                         ReadingGoalRepository goalRepository,
                         UserRepository userRepository) {
        this.userBookRepository = userBookRepository;
        this.reviewRepository = reviewRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    /**
     * Exporta la biblioteca en CSV.
     *
     * <p>El texto se codifica en UTF-8 con BOM para que Excel reconozca las
     * tildes al abrirlo con doble clic.</p>
     *
     * @param userId usuario propietario
     * @return contenido CSV listo para descargar
     */
    @Transactional(readOnly = true)
    public byte[] csv(Long userId) {
        List<UserBook> entries = userBookRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, Review> reviews = reviewsByEntry(userId);

        List<LibraryCsvWriter.Row> rows = entries.stream()
                .map(entry -> {
                    Review review = reviews.get(entry.getId());
                    return new LibraryCsvWriter.Row(
                            entry.getBook().getTitle(),
                            entry.getBook().getAuthor(),
                            entry.getStatus(),
                            entry.getCurrentPage(),
                            entry.getTotalPages(),
                            entry.getStartedAt(),
                            entry.getFinishedAt(),
                            entry.getBook().getIsbn(),
                            entry.getBook().getPublishYear(),
                            review == null ? null : review.getRating().intValue());
                })
                .toList();

        String csv = "\uFEFF" + LibraryCsvWriter.write(rows);
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Exporta la biblioteca completa en JSON.
     *
     * @param userId usuario propietario
     * @return documento con libros, reseñas y metas
     */
    @Transactional(readOnly = true)
    public LibraryExportResponse json(Long userId) {
        List<UserBook> entries = userBookRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, Review> reviews = reviewsByEntry(userId);

        List<LibraryExportResponse.BookEntry> books = entries.stream()
                .map(entry -> {
                    Review review = reviews.get(entry.getId());
                    return new LibraryExportResponse.BookEntry(
                            BookResponse.from(entry.getBook()),
                            entry.getStatus(),
                            entry.getCurrentPage(),
                            entry.getTotalPages(),
                            UserBookResponse.percent(entry.getCurrentPage(), entry.getTotalPages()),
                            entry.getStartedAt(),
                            entry.getFinishedAt(),
                            entry.getCreatedAt(),
                            review == null ? null : new LibraryExportResponse.ReviewEntry(
                                    review.getRating().intValue(),
                                    intOrNull(review.getPaceRating()),
                                    intOrNull(review.getCharactersRating()),
                                    intOrNull(review.getUsefulnessRating()),
                                    review.getContent(),
                                    review.getCreatedAt(),
                                    review.getUpdatedAt()));
                })
                .toList();

        List<LibraryExportResponse.GoalEntry> goals =
                goalRepository.findByUserIdOrderByYearDescMonthDesc(userId).stream()
                        .map(this::toGoal)
                        .toList();

        String username = userRepository.findById(userId)
                .map(user -> user.getUsername())
                .orElse("");

        return new LibraryExportResponse(
                Instant.now(), username, books.size(), books, goals);
    }

    private Map<Long, Review> reviewsByEntry(Long userId) {
        return reviewRepository.findByUserBookUserId(userId).stream()
                .collect(Collectors.toMap(
                        review -> review.getUserBook().getId(),
                        Function.identity()));
    }

    private LibraryExportResponse.GoalEntry toGoal(ReadingGoal goal) {
        return new LibraryExportResponse.GoalEntry(
                goal.getPeriod(),
                goal.getYear(),
                goal.getMonth() == null ? null : goal.getMonth().intValue(),
                goal.getTargetBooks(),
                goal.getTargetPages());
    }

    private static Integer intOrNull(Short value) {
        return value == null ? null : value.intValue();
    }
}
