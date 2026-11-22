package com.librolog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Libro del catálogo o de la caché de Open Library.
 *
 * <p>Los datos se obtienen de Open Library y se guardan para no repetir
 * peticiones innecesarias a la API pública.</p>
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "open_library_key", unique = true, length = 100)
    private String openLibraryKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String author;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(length = 20)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String genre;

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

     * Identificador del libro

     *

     * @return identificador del libro

     */
    public Long getId() { return id; }

    /**

     * Establece Identificador del libro

     *

     * @param id identificador del libro

     */
    public void setId(Long id) { this.id = id; }

    /**

     * Clave de Open Library

     *

     * @return clave de Open Library

     */
    public String getOpenLibraryKey() { return openLibraryKey; }

    /**

     * Establece Clave de Open Library

     *

     * @param openLibraryKey clave de Open Library

     */
    public void setOpenLibraryKey(String openLibraryKey) { this.openLibraryKey = openLibraryKey; }

    /**

     * Título

     *

     * @return título

     */
    public String getTitle() { return title; }

    /**

     * Establece Título

     *

     * @param title título

     */
    public void setTitle(String title) { this.title = title; }

    /**

     * Autor

     *

     * @return autor

     */
    public String getAuthor() { return author; }

    /**

     * Establece Autor

     *

     * @param author autor

     */
    public void setAuthor(String author) { this.author = author; }

    /**

     * URL de la portada

     *

     * @return URL de la portada

     */
    public String getCoverUrl() { return coverUrl; }

    /**

     * Establece URL de la portada

     *

     * @param coverUrl URL de la portada

     */
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    /**

     * Número total de páginas

     *

     * @return número total de páginas

     */
    public Integer getTotalPages() { return totalPages; }

    /**

     * Establece Número total de páginas

     *

     * @param totalPages número total de páginas

     */
    public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }

    /**

     * Año de publicación

     *

     * @return año de publicación

     */
    public Integer getPublishYear() { return publishYear; }

    /**

     * Establece Año de publicación

     *

     * @param publishYear año de publicación

     */
    public void setPublishYear(Integer publishYear) { this.publishYear = publishYear; }

    /**

     * ISBN

     *

     * @return ISBN

     */
    public String getIsbn() { return isbn; }

    /**

     * Establece ISBN

     *

     * @param isbn ISBN

     */
    public void setIsbn(String isbn) { this.isbn = isbn; }

    /**

     * Descripción

     *

     * @return descripción

     */
    public String getDescription() { return description; }

    /**

     * Establece Descripción

     *

     * @param description descripción

     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Género principal
     *
     * @return género principal, puede ser nulo
     */
    public String getGenre() { return genre; }

    /**
     * Establece el género principal
     *
     * @param genre género principal
     */
    public void setGenre(String genre) { this.genre = genre; }

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
