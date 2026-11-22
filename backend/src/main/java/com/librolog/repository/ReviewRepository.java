package com.librolog.repository;

import com.librolog.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de reseñas.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Busca la reseña de una entrada de biblioteca.
     *
     * @param userBookId identificador de la entrada
     * @return reseña, si existe
     */
    Optional<Review> findByUserBookId(Long userBookId);

    /**
     * Lista las reseñas de un usuario con el libro ya cargado.
     *
     * @param userId   identificador del usuario
     * @param pageable paginación y orden
     * @return página de reseñas
     */
    @EntityGraph(attributePaths = {"userBook", "userBook.book"})
    Page<Review> findByUserBookUserId(Long userId, Pageable pageable);

    /**
     * Lista todas las reseñas de un usuario.
     *
     * <p>Se usa para calcular la calificación media y su distribución en las
     * estadísticas.</p>
     *
     * @param userId identificador del usuario
     * @return reseñas del usuario
     */
    List<Review> findByUserBookUserId(Long userId);
}
