package com.librolog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Registro de páginas leídas en un día.
 *
 * <p>Normalmente lo inserta el trigger {@code trg_user_books_after_update}
 * cuando el usuario aumenta la página actual de un libro.</p>
 */
@Entity
@Table(name = "reading_logs")
public class ReadingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_book_id", nullable = false)
    private UserBook userBook;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "pages_read", nullable = false)
    private Integer pagesRead;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Inicializa la fecha del registro antes de insertar.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (logDate == null) {
            logDate = LocalDate.now();
        }
    }

    /**

     * Identificador del registro

     *

     * @return identificador del registro

     */
    public Long getId() { return id; }

    /**

     * Establece Identificador del registro

     *

     * @param id identificador del registro

     */
    public void setId(Long id) { this.id = id; }

    /**

     * Entrada de biblioteca

     *

     * @return entrada de biblioteca

     */
    public UserBook getUserBook() { return userBook; }

    /**

     * Establece Entrada de biblioteca

     *

     * @param userBook entrada de biblioteca

     */
    public void setUserBook(UserBook userBook) { this.userBook = userBook; }

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

     * Fecha del registro

     *

     * @return fecha del registro

     */
    public LocalDate getLogDate() { return logDate; }

    /**

     * Establece Fecha del registro

     *

     * @param logDate fecha del registro

     */
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    /**

     * Páginas leídas

     *

     * @return páginas leídas

     */
    public Integer getPagesRead() { return pagesRead; }

    /**

     * Establece Páginas leídas

     *

     * @param pagesRead páginas leídas

     */
    public void setPagesRead(Integer pagesRead) { this.pagesRead = pagesRead; }

    /**

     * Página actual después del avance

     *

     * @return página actual después del avance

     */
    public Integer getCurrentPage() { return currentPage; }

    /**

     * Establece Página actual después del avance

     *

     * @param currentPage página actual después del avance

     */
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }

    /**

     * Fecha de creación

     *

     * @return fecha de creación

     */
    public Instant getCreatedAt() { return createdAt; }
}
