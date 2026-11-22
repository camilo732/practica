package com.librolog.entity;

import com.librolog.entity.enums.BookStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Relación entre un usuario y un libro de su biblioteca personal.
 *
 * <p>El estado y las fechas las ajusta el trigger de PostgreSQL cuando el
 * usuario actualiza {@code currentPage}.</p>
 */
@Entity
@Table(name = "user_books")
public class UserBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.WANT_TO_READ;

    @Column(name = "current_page", nullable = false)
    private int currentPage = 0;

    @Column(name = "total_pages", nullable = false)
    private int totalPages = 0;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "finished_at")
    private LocalDate finishedAt;

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

     * Identificador de la entrada

     *

     * @return identificador de la entrada

     */
    public Long getId() { return id; }

    /**

     * Establece Identificador de la entrada

     *

     * @param id identificador de la entrada

     */
    public void setId(Long id) { this.id = id; }

    /**

     * Usuario propietario

     *

     * @return usuario propietario

     */
    public User getUser() { return user; }

    /**

     * Establece Usuario propietario

     *

     * @param user usuario propietario

     */
    public void setUser(User user) { this.user = user; }

    /**

     * Libro asociado

     *

     * @return libro asociado

     */
    public Book getBook() { return book; }

    /**

     * Establece Libro asociado

     *

     * @param book libro asociado

     */
    public void setBook(Book book) { this.book = book; }

    /**

     * Estado de lectura

     *

     * @return estado de lectura

     */
    public BookStatus getStatus() { return status; }

    /**

     * Establece Estado de lectura

     *

     * @param status estado de lectura

     */
    public void setStatus(BookStatus status) { this.status = status; }

    /**

     * Página actual

     *

     * @return página actual

     */
    public int getCurrentPage() { return currentPage; }

    /**

     * Establece Página actual

     *

     * @param currentPage página actual

     */
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    /**

     * Total de páginas de esta edición

     *

     * @return total de páginas de esta edición

     */
    public int getTotalPages() { return totalPages; }

    /**

     * Establece Total de páginas de esta edición

     *

     * @param totalPages total de páginas de esta edición

     */
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    /**

     * Fecha de inicio

     *

     * @return fecha de inicio

     */
    public LocalDate getStartedAt() { return startedAt; }

    /**

     * Establece Fecha de inicio

     *

     * @param startedAt fecha de inicio

     */
    public void setStartedAt(LocalDate startedAt) { this.startedAt = startedAt; }

    /**

     * Fecha de finalización

     *

     * @return fecha de finalización

     */
    public LocalDate getFinishedAt() { return finishedAt; }

    /**

     * Establece Fecha de finalización

     *

     * @param finishedAt fecha de finalización

     */
    public void setFinishedAt(LocalDate finishedAt) { this.finishedAt = finishedAt; }

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
