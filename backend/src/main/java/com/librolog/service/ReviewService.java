package com.librolog.service;

import com.librolog.dto.request.ReviewRequest;
import com.librolog.dto.response.PageResponse;
import com.librolog.dto.response.ReviewResponse;
import com.librolog.entity.Review;
import com.librolog.entity.UserBook;
import com.librolog.entity.enums.BookStatus;
import com.librolog.exception.ConflictException;
import com.librolog.repository.ReviewRepository;
import com.librolog.repository.UserBookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Gestión de reseñas estructuradas.
 *
 * <p>Cada entrada de biblioteca admite una sola reseña, así que el alta y la
 * modificación comparten el mismo endpoint idempotente. Solo se puede reseñar
 * un libro terminado; una reseña ya escrita se puede editar o borrar aunque el
 * libro vuelva a abrirse.</p>
 */
@Service
public class ReviewService {

    /** Tamaño máximo de página. */
    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviewRepository;
    private final UserBookRepository userBookRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param reviewRepository   repositorio de reseñas
     * @param userBookRepository repositorio de la biblioteca
     */
    public ReviewService(ReviewRepository reviewRepository,
                         UserBookRepository userBookRepository) {
        this.reviewRepository = reviewRepository;
        this.userBookRepository = userBookRepository;
    }

    /**
     * Crea o reemplaza la reseña de una entrada de biblioteca.
     *
     * @param userId     usuario propietario
     * @param userBookId entrada de biblioteca
     * @param request    calificaciones y texto
     * @return reseña guardada
     * @throws NoSuchElementException si la entrada no existe o es de otra persona
     * @throws ConflictException      si se intenta reseñar un libro sin terminar
     */
    @Transactional
    public ReviewResponse upsert(Long userId, Long userBookId, ReviewRequest request) {
        UserBook entry = findOwned(userId, userBookId);
        Review review = reviewRepository.findByUserBookId(userBookId).orElse(null);

        if (review == null) {
            if (entry.getStatus() != BookStatus.READ) {
                throw new ConflictException(
                        "Solo puedes reseñar libros que hayas terminado.");
            }
            review = new Review();
            review.setUserBook(entry);
        }

        apply(review, request);
        reviewRepository.save(review);
        return ReviewResponse.from(review);
    }

    /**
     * Devuelve la reseña de una entrada propia.
     *
     * @param userId     usuario propietario
     * @param userBookId entrada de biblioteca
     * @return reseña encontrada
     * @throws NoSuchElementException si la entrada o la reseña no existen
     */
    @Transactional(readOnly = true)
    public ReviewResponse get(Long userId, Long userBookId) {
        findOwned(userId, userBookId);
        return reviewRepository.findByUserBookId(userBookId)
                .map(ReviewResponse::from)
                .orElseThrow(() -> new NoSuchElementException(
                        "Ese libro todavía no tiene reseña."));
    }

    /**
     * Lista las reseñas del usuario, de la más reciente a la más antigua.
     *
     * @param userId usuario propietario
     * @param page   página, empezando en 0
     * @param size   tamaño de página
     * @return página de reseñas
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> list(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(MAX_PAGE_SIZE, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Review> result = reviewRepository.findByUserBookUserId(userId, pageable);
        return PageResponse.from(result.map(ReviewResponse::from));
    }

    /**
     * Elimina la reseña de una entrada propia.
     *
     * @param userId     usuario propietario
     * @param userBookId entrada de biblioteca
     * @throws NoSuchElementException si la entrada o la reseña no existen
     */
    @Transactional
    public void delete(Long userId, Long userBookId) {
        findOwned(userId, userBookId);
        Review review = reviewRepository.findByUserBookId(userBookId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Ese libro todavía no tiene reseña."));
        reviewRepository.delete(review);
    }

    private UserBook findOwned(Long userId, Long userBookId) {
        return userBookRepository.findByIdAndUserId(userBookId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe ese libro en tu biblioteca."));
    }

    private void apply(Review review, ReviewRequest request) {
        review.setRating(request.rating().shortValue());
        review.setPaceRating(shortOrNull(request.paceRating()));
        review.setCharactersRating(shortOrNull(request.charactersRating()));
        review.setUsefulnessRating(shortOrNull(request.usefulnessRating()));
        review.setContent(trimToNull(request.content()));
    }

    private static Short shortOrNull(Integer value) {
        return value == null ? null : value.shortValue();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
