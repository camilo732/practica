package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado de una búsqueda en el catálogo.
 *
 * @param items     libros encontrados
 * @param total     número de resultados devueltos
 * @param fromCache verdadero si se respondió con la caché local porque Open
 *                  Library no estaba disponible
 */
@Schema(description = "Resultado de búsqueda de libros")
public record BookSearchResponse(

        @Schema(description = "Libros encontrados")
        List<BookResponse> items,

        @Schema(description = "Número de resultados", example = "10")
        int total,

        @Schema(description = "Respondido desde la caché local", example = "false")
        boolean fromCache) {
}
