package com.librolog.client;

/**
 * Libro tal y como lo devuelve Open Library, antes de guardarlo en la caché.
 *
 * @param openLibraryKey clave de la obra, por ejemplo {@code /works/OL12345W}
 * @param title          título
 * @param author         primera autoría disponible
 * @param coverUrl       URL de la portada en tamaño medio
 * @param totalPages     mediana de páginas entre las ediciones
 * @param publishYear    año de la primera publicación
 * @param isbn           primer ISBN disponible
 * @param description    descripción, solo si se consultó el detalle de la obra
 * @param genre          primer tema útil de la obra, usado como género
 */
public record OpenLibraryBook(
        String openLibraryKey,
        String title,
        String author,
        String coverUrl,
        Integer totalPages,
        Integer publishYear,
        String isbn,
        String description,
        String genre) {
}
