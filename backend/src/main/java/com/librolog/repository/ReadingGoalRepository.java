package com.librolog.repository;

import com.librolog.entity.ReadingGoal;
import com.librolog.entity.enums.GoalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de metas de lectura.
 */
public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Long> {

    /**
     * Lista todas las metas de un usuario, del año más reciente al más antiguo.
     *
     * <p>Dentro de cada año, la meta anual (mes nulo) aparece antes que las
     * mensuales, que van de diciembre a enero.</p>
     *
     * @param userId identificador del usuario
     * @return metas del usuario
     */
    List<ReadingGoal> findByUserIdOrderByYearDescMonthDesc(Long userId);

    /**
     * Lista las metas de un año concreto.
     *
     * @param userId identificador del usuario
     * @param year   año
     * @return metas del año
     */
    List<ReadingGoal> findByUserIdAndYearOrderByMonthDesc(Long userId, Integer year);

    /**
     * Busca una meta concreta comprobando la propiedad.
     *
     * @param id     identificador de la meta
     * @param userId identificador del usuario propietario
     * @return meta encontrada, si pertenece a ese usuario
     */
    Optional<ReadingGoal> findByIdAndUserId(Long id, Long userId);

    /**
     * Cuenta las metas que ya existen para un periodo concreto.
     *
     * <p>El mes nulo identifica las metas anuales, así que la consulta compara
     * nulos explícitamente.</p>
     *
     * @param userId identificador del usuario
     * @param period periodicidad
     * @param year   año
     * @param month  mes, o {@code null} para una meta anual
     * @return número de metas existentes
     */
    @Query("""
            select count(g)
              from ReadingGoal g
             where g.user.id = :userId
               and g.period = :period
               and g.year = :year
               and ((:month is null and g.month is null) or g.month = :month)
            """)
    long countExisting(@Param("userId") Long userId,
                       @Param("period") GoalPeriod period,
                       @Param("year") Integer year,
                       @Param("month") Short month);

    /**
     * Cuenta las cuentas que tienen al menos una meta en un año (uso global).
     *
     * @param year año de la meta
     * @return número de cuentas distintas
     */
    @Query("""
            select count(distinct g.user.id)
              from ReadingGoal g
             where g.year = :year
            """)
    long countUsersWithGoalInYear(@Param("year") Integer year);
}
