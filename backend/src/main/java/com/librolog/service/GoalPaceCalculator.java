package com.librolog.service;

import com.librolog.entity.enums.GoalPeriod;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Cálculo del ritmo de una meta de lectura.
 *
 * <p>Es una clase pura, sin Spring ni base de datos, para poder probarla con
 * fechas fijas. Compara el avance real con el que correspondería a la altura del
 * periodo: un 50 % del objetivo en julio de una meta anual es ir al ritmo
 * previsto; un 30 % no lo es.</p>
 */
public final class GoalPaceCalculator {

    /** Días medios de un mes, para convertir días restantes en meses. */
    private static final double DAYS_PER_MONTH = 30.4375;

    private GoalPaceCalculator() {
    }

    /**
     * Límites de un periodo.
     *
     * @param start primer día, incluido
     * @param end   último día, incluido
     */
    public record Bounds(LocalDate start, LocalDate end) {
    }

    /**
     * Resultado del cálculo de ritmo.
     *
     * @param periodStart             primer día del periodo
     * @param periodEnd               último día del periodo
     * @param daysTotal               días que dura el periodo
     * @param daysElapsed             días transcurridos, contando hoy
     * @param daysRemaining           días que quedan, contando hoy
     * @param booksRead               libros terminados dentro del periodo
     * @param pagesRead               páginas leídas dentro del periodo
     * @param booksPercent            porcentaje de libros, limitado a 100
     * @param pagesPercent            porcentaje de páginas, limitado a 100
     * @param expectedPercent         porcentaje que correspondería a la fecha
     * @param onTrack                 verdadero si el avance va al ritmo esperado
     * @param expectedBooks           libros que corresponderían a la fecha
     * @param booksRemaining          libros que faltan
     * @param pagesRemaining          páginas que faltan
     * @param requiredBooksPerMonth   ritmo necesario de libros por mes
     * @param requiredPagesPerDay     ritmo necesario de páginas por día
     */
    public record Pace(
            LocalDate periodStart,
            LocalDate periodEnd,
            int daysTotal,
            int daysElapsed,
            int daysRemaining,
            long booksRead,
            long pagesRead,
            int booksPercent,
            int pagesPercent,
            int expectedPercent,
            boolean onTrack,
            double expectedBooks,
            long booksRemaining,
            long pagesRemaining,
            double requiredBooksPerMonth,
            double requiredPagesPerDay) {
    }

    /**
     * Calcula los límites del periodo de una meta.
     *
     * @param period periodicidad
     * @param year   año
     * @param month  mes, solo para las metas mensuales
     * @return primer y último día del periodo
     * @throws IllegalArgumentException si una meta mensual no trae mes
     */
    public static Bounds bounds(GoalPeriod period, int year, Integer month) {
        if (period == GoalPeriod.ANNUAL) {
            return new Bounds(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        }
        if (month == null) {
            throw new IllegalArgumentException("Las metas mensuales necesitan un mes.");
        }
        YearMonth yearMonth = YearMonth.of(year, month);
        return new Bounds(yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    /**
     * Calcula el avance y el ritmo de una meta.
     *
     * @param period      periodicidad
     * @param year        año
     * @param month       mes, solo para las metas mensuales
     * @param targetBooks libros objetivo
     * @param targetPages páginas objetivo
     * @param booksRead   libros terminados en el periodo
     * @param pagesRead   páginas leídas en el periodo
     * @param today       fecha con la que se compara
     * @return avance, ritmo esperado y ritmo necesario
     * @throws IllegalArgumentException si una meta mensual no trae mes
     */
    public static Pace calculate(GoalPeriod period, int year, Integer month,
                                 int targetBooks, int targetPages,
                                 long booksRead, long pagesRead,
                                 LocalDate today) {

        Bounds bounds = bounds(period, year, month);
        int daysTotal = (int) ChronoUnit.DAYS.between(bounds.start(), bounds.end()) + 1;

        LocalDate effective = today.isBefore(bounds.start())
                ? bounds.start()
                : (today.isAfter(bounds.end()) ? bounds.end() : today);

        int daysElapsed = today.isBefore(bounds.start())
                ? 0
                : (int) ChronoUnit.DAYS.between(bounds.start(), effective) + 1;
        int daysRemaining = today.isAfter(bounds.end())
                ? 0
                : (int) ChronoUnit.DAYS.between(effective, bounds.end()) + 1;

        int expectedPercent = (int) Math.round(100.0 * daysElapsed / daysTotal);
        int booksPercent = percent(booksRead, targetBooks);
        int pagesPercent = percent(pagesRead, targetPages);

        long booksRemaining = Math.max(0, (long) targetBooks - booksRead);
        long pagesRemaining = Math.max(0, (long) targetPages - pagesRead);

        double expectedBooks = round1(targetBooks * (double) daysElapsed / daysTotal);
        double requiredBooksPerMonth = daysRemaining == 0
                ? booksRemaining
                : round1(booksRemaining / (daysRemaining / DAYS_PER_MONTH));
        double requiredPagesPerDay = daysRemaining == 0
                ? pagesRemaining
                : round1(pagesRemaining / (double) daysRemaining);

        return new Pace(
                bounds.start(),
                bounds.end(),
                daysTotal,
                daysElapsed,
                daysRemaining,
                booksRead,
                pagesRead,
                booksPercent,
                pagesPercent,
                expectedPercent,
                booksPercent >= expectedPercent,
                expectedBooks,
                booksRemaining,
                pagesRemaining,
                requiredBooksPerMonth,
                requiredPagesPerDay);
    }

    private static int percent(long value, long target) {
        if (target <= 0) {
            return 0;
        }
        long result = Math.round(value * 100.0 / target);
        return (int) Math.max(0, Math.min(100, result));
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
