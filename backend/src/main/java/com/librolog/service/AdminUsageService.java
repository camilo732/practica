package com.librolog.service;

import com.librolog.dto.response.AdminUsageResponse;
import com.librolog.entity.enums.BookStatus;
import com.librolog.repository.ReadingGoalRepository;
import com.librolog.repository.ReadingLogRepository;
import com.librolog.repository.ReviewRepository;
import com.librolog.repository.UserBookRepository;
import com.librolog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Reportes globales de uso para el administrador.
 *
 * <p>Cumple la regla del anteproyecto: el administrador consulta indicadores
 * de la plataforma, pero nunca el contenido privado de las bibliotecas ni las
 * reseñas. Todas las consultas son {@code count} o {@code sum}.</p>
 */
@Service
public class AdminUsageService {

    private final UserRepository userRepository;
    private final UserBookRepository userBookRepository;
    private final ReviewRepository reviewRepository;
    private final ReadingGoalRepository readingGoalRepository;
    private final ReadingLogRepository readingLogRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository        repositorio de usuarios
     * @param userBookRepository    repositorio de bibliotecas
     * @param reviewRepository      repositorio de reseñas
     * @param readingGoalRepository repositorio de metas
     * @param readingLogRepository  repositorio de la bitácora
     */
    public AdminUsageService(UserRepository userRepository,
                             UserBookRepository userBookRepository,
                             ReviewRepository reviewRepository,
                             ReadingGoalRepository readingGoalRepository,
                             ReadingLogRepository readingLogRepository) {
        this.userRepository = userRepository;
        this.userBookRepository = userBookRepository;
        this.reviewRepository = reviewRepository;
        this.readingGoalRepository = readingGoalRepository;
        this.readingLogRepository = readingLogRepository;
    }

    /**
     * Calcula el reporte global de uso.
     *
     * @return indicadores agregados de la plataforma
     */
    @Transactional(readOnly = true)
    public AdminUsageResponse usage() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        long users = userRepository.count();
        long entries = userBookRepository.count();
        long finished = userBookRepository.countByStatus(BookStatus.READ);
        long reviews = reviewRepository.count();
        long withGoal = readingGoalRepository.countUsersWithGoalInYear(today.getYear());
        long active = readingLogRepository.countActiveUsersSince(startOfMonth);
        long pages = readingLogRepository.sumPagesSince(startOfMonth);

        return new AdminUsageResponse(
                today.getYear(),
                entries,
                finished,
                ratio(entries, users),
                reviews,
                withGoal,
                users == 0 ? 0.0 : ratio(withGoal * 100, users),
                active,
                pages);
    }

    private static double ratio(long value, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(value * 10.0 / total) / 10.0;
    }
}
