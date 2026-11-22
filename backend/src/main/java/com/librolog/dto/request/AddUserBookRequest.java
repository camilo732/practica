package com.librolog.dto.request;

import com.librolog.entity.enums.BookStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos para añadir un libro a la biblioteca personal.
 *
 * <p>Hay tres formas de uso, en orden de preferencia:</p>
 * <ol>
 *   <li>{@code bookId}: el libro ya está en la caché local (lo habitual tras
 *       una búsqueda).</li>
 *   <li>{@code openLibraryKey}: se busca en la caché y, si no está, se consulta
 *       Open Library para completar los datos.</li>
 *   <li>{@code title} y {@code author}: alta manual de un libro que no aparece
 *       en Open Library.</li>
 * </ol>
 *
 * @param bookId         identificador interno del libro ya cacheado
 * @param openLibraryKey clave de Open Library, por ejemplo {@code /works/OL12345W}
 * @param title          título para el alta manual
 * @param author         autoría para el alta manual
 * @param totalPages     total de páginas de la edición
 * @param publishYear    año de publicación
 * @param isbn           ISBN
 * @param coverUrl       URL de la portada
 * @param status         estado inicial; por defecto {@code WANT_TO_READ}
 * @param currentPage    página actual; por defecto 0
 */
@Schema(description = "Datos para añadir un libro a la biblioteca personal")
public record AddUserBookRequest(

        @Schema(description = "Identificador de un libro ya cacheado", example = "12")
        @PositiveOrZero(message = "El identificador del libro no puede ser negativo.")
        Long bookId,

        @Schema(description = "Clave de Open Library", example = "/works/OL12345W")
        @Pattern(regexp = "^/works/OL[A-Za-z0-9]+W$",
                message = "La clave de Open Library no tiene un formato válido.")
        String openLibraryKey,

        @Schema(description = "Título (alta manual)", example = "Cien años de soledad")
        @Size(max = 500, message = "El título no puede superar 500 caracteres.")
        String title,

        @Schema(description = "Autoría (alta manual)", example = "Gabriel García Márquez")
        @Size(max = 500, message = "La autoría no puede superar 500 caracteres.")
        String author,

        @Schema(description = "Total de páginas", example = "471")
        @Min(value = 1, message = "El total de páginas debe ser al menos 1.")
        Integer totalPages,

        @Schema(description = "Año de publicación", example = "1967")
        @Min(value = 1000, message = "El año de publicación no es válido.")
        Integer publishYear,

        @Schema(description = "ISBN", example = "9788437604947")
        @Size(max = 20, message = "El ISBN no puede superar 20 caracteres.")
        String isbn,

        @Schema(description = "URL de la portada")
        @Size(max = 1000, message = "La URL de la portada no puede superar 1000 caracteres.")
        String coverUrl,

        @Schema(description = "Estado inicial", example = "WANT_TO_READ")
        BookStatus status,

        @Schema(description = "Página actual", example = "0")
        @Min(value = 0, message = "La página actual no puede ser negativa.")
        Integer currentPage) {
}
