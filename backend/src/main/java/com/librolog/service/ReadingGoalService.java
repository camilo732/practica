package com.librolog.service;

import com.librolog.dto.request.ReadingGoalRequest;
import com.librolog.dto.request.UpdateReadingGoalRequest;
import com.librolog.dto.response.ReadingGoalResponse;
import com.librolog.entity.ReadingGoal;
import com.librolog.entity.enums.BookStatus;
import com.librolog.entity.enums.GoalPeriod;
import com.librolog.exception.ConflictException;
import com.librolog.repository.ReadingGoalRepository;
import com.librolog.repository.ReadingLogRepository;
import com.librolog.repository.UserBookRepository;
import com.librolog.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Gestión de metas de lectura anuales y mensuales.
 *
 * <p>El avance no se guarda: se calcula en cada consulta con
 * {@link GoalPaceCalculator} a partir de los libros terminados y de la bitácora
 * de páginas. Así una meta nunca muestra datos obsoletos.</p>
 */
@Service
public class ReadingGoalService {

    private static final Locale SPANISH = Locale.forLanguageTag("es");

    private final ReadingGoalRepository goalRepository;
    private final UserBookRepository userBookRepository;
    private final ReadingLogRepository readingLogRepository;
    private final UserRepository userRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param goalRepository       repositorio de metas
     * @param userBookRepository   repositorio de la biblioteca
     * @param readingLogRepository repositorio de la bitácora
     * @param userRepository       repositorio de usuarios
     */
    public ReadingGoalService(ReadingGoalRepository goalRepository,
                              UserBookRepository userBookRepository,
                              ReadingLogRepository readingLogRepository,
                              UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userBookRepository = userBookRepository;
        this.readingLogRepository = readingLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crea una meta para un periodo.
     *
     * @param userId  usuario propietario
     * @param request datos de la meta
     * @return meta con su avance inicial
     * @throws IllegalArgumentException si el mes no corresponde a la periodicidad
     * @throws ConflictException        si ya existe una meta para ese periodo
     */
    @Transactional
    public ReadingGoalResponse create(Long userId, ReadingGoalRequest request) {
        validatePeriod(request.period(), request.month());
        Short month = toMonth(request.month());

        if (goalRepository.countExisting(userId, request.period(), request.year(), month) > 0) {
            throw new ConflictException(
                    "Ya tienes una meta " + describe(request.period(), request.year(), request.month()) + ".");
        }

        ReadingGoal goal = new ReadingGoal();
        goal.setUser(userRepository.getReferenceById(userId));
        goal.setPeriod(request.period());
        goal.setYear(request.year());
        goal.setMonth(month);
        goal.setTargetBooks(request.targetBooks());
        goal.setTargetPages(request.targetPages());
        goalRepository.save(goal);

        return buildResponse(goal, userId);
    }

    /**
     * Lista las metas del usuario, opcionalmente filtradas por año.
     *
     * @param userId usuario propietario
     * @param year   año, o {@code null} para todas
     * @return metas con su avance
     */
    @Transactional(readOnly = true)
    public List<ReadingGoalResponse> list(Long userId, Integer year) {
        List<ReadingGoal> goals = year == null
                ? goalRepository.findByUserIdOrderByYearDescMonthDesc(userId)
                : goalRepository.findByUserIdAndYearOrderByMonthDesc(userId, year);

        return goals.stream()
                .map(goal -> buildResponse(goal, userId))
                .toList();
    }

    /**
     * Devuelve una meta concreta con su avance.
     *
     * @param userId usuario propietario
     * @param id     identificador de la meta
     * @return meta con su avance
     * @throws NoSuchElementException si no existe o es de otra persona
     */
    @Transactional(readOnly = true)
    public ReadingGoalResponse get(Long userId, Long id) {
        return buildResponse(findOwned(userId, id), userId);
    }

    /**
     * Modifica los objetivos de una meta.
     *
     * @param userId  usuario propietario
     * @param id      identificador de la meta
     * @param request nuevos objetivos
     * @return meta actualizada con su avance
     * @throws NoSuchElementException si no existe o es de otra persona
     */
    @Transactional
    public ReadingGoalResponse update(Long userId, Long id, UpdateReadingGoalRequest request) {
        ReadingGoal goal = findOwned(userId, id);
        goal.setTargetBooks(request.targetBooks());
        goal.setTargetPages(request.targetPages());
        goalRepository.save(goal);

        return buildResponse(goal, userId);
    }

    /**
     * Elimina una meta.
     *
     * @param userId usuario propietario
     * @param id     identificador de la meta
     * @throws NoSuchElementException si no existe o es de otra persona
     */
    @Transactional
    public void delete(Long userId, Long id) {
        goalRepository.delete(findOwned(userId, id));
    }

    private ReadingGoal findOwned(Long userId, Long id) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("No existe esa meta de lectura."));
    }

    private ReadingGoalResponse buildResponse(ReadingGoal goal, Long userId) {
        Integer month = goal.getMonth() == null ? null : goal.getMonth().intValue();
        GoalPaceCalculator.Bounds bounds =
                GoalPaceCalculator.bounds(goal.getPeriod(), goal.getYear(), month);

        long booksRead = userBookRepository.countFinishedBetween(
                userId, BookStatus.READ, bounds.start(), bounds.end());
        long pagesRead = readingLogRepository.sumPagesBetween(
                userId, bounds.start(), bounds.end());

        GoalPaceCalculator.Pace pace = GoalPaceCalculator.calculate(
                goal.getPeriod(),
                goal.getYear(),
                month,
                goal.getTargetBooks(),
                goal.getTargetPages(),
                booksRead,
                pagesRead,
                LocalDate.now());

        return ReadingGoalResponse.from(goal, pace);
    }

    private static void validatePeriod(GoalPeriod period, Integer month) {
        if (period == GoalPeriod.ANNUAL && month != null) {
            throw new IllegalArgumentException("Las metas anuales no llevan mes.");
        }
        if (period == GoalPeriod.MONTHLY && month == null) {
            throw new IllegalArgumentException("Las metas mensuales necesitan un mes entre 1 y 12.");
        }
    }

    private static Short toMonth(Integer month) {
        return month == null ? null : month.shortValue();
    }

    private static String describe(GoalPeriod period, int year, Integer month) {
        if (period == GoalPeriod.ANNUAL) {
            return "anual para " + year;
        }
        String name = Month.of(month).getDisplayName(TextStyle.FULL, SPANISH);
        return "para " + name + " de " + year;
    }
}
