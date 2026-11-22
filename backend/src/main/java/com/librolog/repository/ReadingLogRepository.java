package com.librolog.repository;

import com.librolog.entity.ReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de la bitácora de páginas leídas.
 *
 * <p>Las filas las inserta el trigger de PostgreSQL al aumentar la página
 * actual; la aplicación solo las consulta.</p>
 */
public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {

    /**
     * Suma las páginas leídas por un usuario en un intervalo de fechas.
     *
     * @param userId identificador del usuario
     * @param start  primer día del intervalo, incluido
     * @param end    último día del intervalo, incluido
     * @return total de páginas; 0 si no hay registros
     */
    @Query("""
            select coalesce(sum(rl.pagesRead), 0)
              from ReadingLog rl
             where rl.user.id = :userId
               and rl.logDate between :start and :end
            """)
    long sumPagesBetween(@Param("userId") Long userId,
                         @Param("start") LocalDate start,
                         @Param("end") LocalDate end);

    /**
     * Suma todas las páginas leídas por un usuario.
     *
     * @param userId identificador del usuario
     * @return total histórico de páginas; 0 si no hay registros
     */
    @Query("""
            select coalesce(sum(rl.pagesRead), 0)
              from ReadingLog rl
             where rl.user.id = :userId
            """)
    long sumPagesByUser(@Param("userId") Long userId);

    /**
     * Lista los registros de un usuario dentro de un año.
     *
     * <p>Se consultan los registros completos porque las estadísticas necesitan
     * la fecha y las páginas de cada fila para agruparlas por mes.</p>
     *
     * @param userId identificador del usuario
     * @param start  primer día del año, incluido
     * @param end    último día del año, incluido
     * @return registros ordenados por fecha
     */
    List<ReadingLog> findByUserIdAndLogDateBetweenOrderByLogDateAsc(
            Long userId, LocalDate start, LocalDate end);

    /**
     * Cuenta las cuentas que registraron progreso desde una fecha (uso global).
     *
     * @param since primer día incluido
     * @return número de cuentas distintas
     */
    @Query("""
            select count(distinct rl.user.id)
              from ReadingLog rl
             where rl.logDate >= :since
            """)
    long countActiveUsersSince(@Param("since") LocalDate since);

    /**
     * Suma las páginas registradas en toda la plataforma desde una fecha.
     *
     * @param since primer día incluido
     * @return total de páginas; 0 si no hay registros
     */
    @Query("""
            select coalesce(sum(rl.pagesRead), 0)
              from ReadingLog rl
             where rl.logDate >= :since
            """)
    long sumPagesSince(@Param("since") LocalDate since);
}
