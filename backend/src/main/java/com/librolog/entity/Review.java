package com.librolog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Reseña estructurada de un libro leído.
 *
 * <p>Incluye una calificación general y tres subcalificaciones opcionales:
 * ritmo, personajes y utilidad.</p>
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_book_id", nullable = false, unique = true)
    private UserBook userBook;

    @Column(nullable = false)
    private Short rating;

    @Column(name = "pace_rating")
    private Short paceRating;

    @Column(name = "characters_rating")
    private Short charactersRating;

    @Column(name = "usefulness_rating")
    private Short usefulnessRating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Inicializa las fechas antes de insertar.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    /**
     * Actualiza la fecha antes de modificar.
     */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /**

     * Identificador de la reseña

     *

     * @return identificador de la reseña

     */
    public Long getId() { return id; }

    /**

     * Establece Identificador de la reseña

     *

     * @param id identificador de la reseña

     */
    public void setId(Long id) { this.id = id; }

    /**

     * Entrada de biblioteca reseñada

     *

     * @return entrada de biblioteca reseñada

     */
    public UserBook getUserBook() { return userBook; }

    /**

     * Establece Entrada de biblioteca reseñada

     *

     * @param userBook entrada de biblioteca reseñada

     */
    public void setUserBook(UserBook userBook) { this.userBook = userBook; }

    /**

     * Calificación general de 1 a 5

     *

     * @return calificación general de 1 a 5

     */
    public Short getRating() { return rating; }

    /**

     * Establece Calificación general de 1 a 5

     *

     * @param rating calificación general de 1 a 5

     */
    public void setRating(Short rating) { this.rating = rating; }

    /**

     * Calificación de ritmo de 1 a 5

     *

     * @return calificación de ritmo de 1 a 5

     */
    public Short getPaceRating() { return paceRating; }

    /**

     * Establece Calificación de ritmo de 1 a 5

     *

     * @param paceRating calificación de ritmo de 1 a 5

     */
    public void setPaceRating(Short paceRating) { this.paceRating = paceRating; }

    /**

     * Calificación de personajes de 1 a 5

     *

     * @return calificación de personajes de 1 a 5

     */
    public Short getCharactersRating() { return charactersRating; }

    /**

     * Establece Calificación de personajes de 1 a 5

     *

     * @param charactersRating calificación de personajes de 1 a 5

     */
    public void setCharactersRating(Short charactersRating) { this.charactersRating = charactersRating; }

    /**

     * Calificación de utilidad de 1 a 5

     *

     * @return calificación de utilidad de 1 a 5

     */
    public Short getUsefulnessRating() { return usefulnessRating; }

    /**

     * Establece Calificación de utilidad de 1 a 5

     *

     * @param usefulnessRating calificación de utilidad de 1 a 5

     */
    public void setUsefulnessRating(Short usefulnessRating) { this.usefulnessRating = usefulnessRating; }

    /**

     * Texto de la reseña

     *

     * @return texto de la reseña

     */
    public String getContent() { return content; }

    /**

     * Establece Texto de la reseña

     *

     * @param content texto de la reseña

     */
    public void setContent(String content) { this.content = content; }

    /**

     * Fecha de creación

     *

     * @return fecha de creación

     */
    public Instant getCreatedAt() { return createdAt; }

    /**

     * Fecha de última modificación

     *

     * @return fecha de última modificación

     */
    public Instant getUpdatedAt() { return updatedAt; }
}
