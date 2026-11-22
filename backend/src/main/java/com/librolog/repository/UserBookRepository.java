package com.librolog.repository;

import com.librolog.entity.UserBook;
import com.librolog.entity.enums.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la biblioteca personal.
 *
 * <p>Las consultas paginadas usan {@code EntityGraph} para traer el libro en la
 * misma consulta. Sin él, con {@code open-in-view=false}, acceder a la portada o
 * al título lanzaría una consulta por cada fila.</p>
 */
public interface UserBookRepository extends JpaRepository<UserBook, Long> {

    /**
     * Lista la biblioteca de un usuario, del más reciente al más antiguo.
     *
     * @param userId identificador del usuario
     * @return entradas de biblioteca
     */
    @EntityGraph(attributePaths = "book")
    List<UserBook> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Lista la biblioteca de un usuario filtrada por estado.
     *
     * @param userId identificador del usuario
     * @param status estado de lectura
     * @return entradas de biblioteca
     */
    List<UserBook> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, BookStatus status);

    /**
     * Página de biblioteca de un usuario, con el libro ya cargado.
     *
     * @param userId   identificador del usuario
     * @param pageable paginación y orden
     * @return página de entradas
     */
    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUserId(Long userId, Pageable pageable);

    /**
     * Página de biblioteca filtrada por estado, con el libro ya cargado.
     *
     * @param userId   identificador del usuario
     * @param status   estado de lectura
     * @param pageable paginación y orden
     * @return página de entradas
     */
    @EntityGraph(attributePaths = "book")
    Page<UserBook> findByUserIdAndStatus(Long userId, BookStatus status, Pageable pageable);

    /**
     * Busca una entrada concreta y comprueba la propiedad en la misma consulta.
     *
     * <p>Si el libro pertenece a otra persona la consulta no devuelve nada, así
     * que la respuesta es 404 y no se revela su existencia.</p>
     *
     * @param id     identificador de la entrada
     * @param userId identificador del usuario propietario
     * @return entrada encontrada, si pertenece a ese usuario
     */
    @EntityGraph(attributePaths = "book")
    Optional<UserBook> findByIdAndUserId(Long id, Long userId);

    /**
     * Busca una entrada concreta de la biblioteca.
     *
     * @param userId identificador del usuario
     * @param bookId identificador del libro
     * @return entrada encontrada, si existe
     */
    Optional<UserBook> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Comprueba si un usuario ya tiene un libro en su biblioteca.
     *
     * @param userId identificador del usuario
     * @param bookId identificador del libro
     * @return verdadero si ya existe
     */
    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Cuenta los libros de un usuario con un estado concreto.
     *
     * @param userId identificador del usuario
     * @param status estado de lectura
     * @return número de libros
     */
    long countByUserIdAndStatus(Long userId, BookStatus status);

    /**
     * Cuenta todos los libros de un usuario.
     *
     * @param userId identificador del usuario
     * @return número de libros
     */
    long countByUserId(Long userId);

    /**
     * Cuenta los libros terminados dentro de un intervalo de fechas.
     *
     * <p>Se usa para calcular el avance de las metas de lectura. La fecha de fin
     * la escribe el trigger al llegar a la última página.</p>
     *
     * @param userId identificador del usuario
     * @param status estado que se quiere contar
     * @param start  primer día del intervalo, incluido
     * @param end    último día del intervalo, incluido
     * @return número de libros terminados en el intervalo
     */
    @Query("""
            select count(ub)
              from UserBook ub
             where ub.user.id = :userId
               and ub.status = :status
               and ub.finishedAt between :start and :end
            """)
    long countFinishedBetween(@Param("userId") Long userId,
                              @Param("status") BookStatus status,
                              @Param("start") LocalDate start,
                              @Param("end") LocalDate end);

    /**
     * Cuenta los libros de todas las bibliotecas con un estado (uso global).
     *
     * @param status estado del libro
     * @return número de registros
     */
    long countByStatus(BookStatus status);
}
