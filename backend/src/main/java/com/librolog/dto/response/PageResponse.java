package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Página de resultados.
 *
 * @param items         elementos de la página
 * @param page          número de página, empezando en 0
 * @param size          tamaño de página
 * @param totalElements total de elementos
 * @param totalPages    total de páginas
 * @param hasNext       verdadero si hay página siguiente
 * @param <T>           tipo de elemento
 */
@Schema(description = "Página de resultados")
public record PageResponse<T>(

        @Schema(description = "Elementos")
        List<T> items,

        @Schema(description = "Página actual", example = "0")
        int page,

        @Schema(description = "Tamaño de página", example = "20")
        int size,

        @Schema(description = "Total de elementos", example = "42")
        long totalElements,

        @Schema(description = "Total de páginas", example = "3")
        int totalPages,

        @Schema(description = "Hay página siguiente", example = "true")
        boolean hasNext) {

    /**
     * Convierte una página de Spring Data.
     *
     * @param page página original
     * @param <T>  tipo de elemento
     * @return página serializable
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
