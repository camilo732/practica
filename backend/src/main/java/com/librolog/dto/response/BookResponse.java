package com.librolog.dto.response;

import com.librolog.entity.Book;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Datos públicos de un libro.
 *
 * @param id             identificador interno
 * @param openLibraryKey clave de Open Library, si procede
 * @param title          título
 * @param author         autoría
 * @param coverUrl       URL de la portada
 * @param totalPages     número de páginas
 * @param publishYear    año de publicación
 * @param isbn           ISBN
 * @param description    descripción
 */
@Schema(description = "Libro del catálogo")
public record BookResponse(

        @Schema(description = "Identificador interno", example = "12")
        Long id,

        @Schema(description = "Clave de Open Library", example = "/works/OL12345W")
        String openLibraryKey,

        @Schema(description = "Título", example = "Cien años de soledad")
        String title,

        @Schema(description = "Autoría", example = "Gabriel García Márquez")
        String author,

        @Schema(description = "Portada")
        String coverUrl,

        @Schema(description = "Páginas", example = "471")
        Integer totalPages,

        @Schema(description = "Año de publicación", example = "1967")
        Integer publishYear,

        @Schema(description = "ISBN")
        String isbn,

        @Schema(description = "Descripción")
        String description,

        @Schema(description = "Género principal", example = "Fiction")
        String genre) {

    /**
     * Convierte una entidad en su representación pública.
     *
     * @param book entidad de libro
     * @return datos públicos
     */
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getOpenLibraryKey(),
                book.getTitle(),
                book.getAuthor(),
                book.getCoverUrl(),
                book.getTotalPages(),
                book.getPublishYear(),
                book.getIsbn(),
                book.getDescription(),
                book.getGenre());
    }
}
