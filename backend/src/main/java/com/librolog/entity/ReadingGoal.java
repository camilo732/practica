package com.librolog.entity;

import com.librolog.entity.enums.GoalPeriod;
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

/**
 * Meta de lectura anual o mensual de un usuario.
 */
@Entity
@Table(name = "reading_goals")
public class ReadingGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GoalPeriod period;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "\"month\"")
    private Short month;

    @Column(name = "target_books", nullable = false)
    private Integer targetBooks;

    @Column(name = "target_pages", nullable = false)
    private Integer targetPages;

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

     * Identificador de la meta

     *

     * @return identificador de la meta

     */
    public Long getId() { return id; }

    /**

     * Establece Identificador de la meta

     *

     * @param id identificador de la meta

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

     * Periodicidad

     *

     * @return periodicidad

     */
    public GoalPeriod getPeriod() { return period; }

    /**

     * Establece Periodicidad

     *

     * @param period periodicidad

     */
    public void setPeriod(GoalPeriod period) { this.period = period; }

    /**

     * Año de la meta

     *

     * @return año de la meta

     */
    public Integer getYear() { return year; }

    /**

     * Establece Año de la meta

     *

     * @param year año de la meta

     */
    public void setYear(Integer year) { this.year = year; }

    /**

     * Mes (nulo en metas anuales)

     *

     * @return mes (nulo en metas anuales)

     */
    public Short getMonth() { return month; }

    /**

     * Establece Mes (nulo en metas anuales)

     *

     * @param month mes (nulo en metas anuales)

     */
    public void setMonth(Short month) { this.month = month; }

    /**

     * Libros objetivo

     *

     * @return libros objetivo

     */
    public Integer getTargetBooks() { return targetBooks; }

    /**

     * Establece Libros objetivo

     *

     * @param targetBooks libros objetivo

     */
    public void setTargetBooks(Integer targetBooks) { this.targetBooks = targetBooks; }

    /**

     * Páginas objetivo

     *

     * @return páginas objetivo

     */
    public Integer getTargetPages() { return targetPages; }

    /**

     * Establece Páginas objetivo

     *

     * @param targetPages páginas objetivo

     */
    public void setTargetPages(Integer targetPages) { this.targetPages = targetPages; }

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
