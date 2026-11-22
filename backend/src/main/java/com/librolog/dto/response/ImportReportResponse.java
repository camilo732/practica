package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado de una importación CSV.
 *
 * <p>Una fila con errores no detiene el resto: el informe indica cuántas se
 * importaron, cuántas se omitieron por estar repetidas y cuáles fallaron.</p>
 *
 * @param format          formato detectado: {@code LIBROLOG} o {@code GOODREADS}
 * @param totalRows       filas de datos leídas
 * @param importedBooks   libros añadidos a la biblioteca
 * @param importedReviews reseñas creadas a partir de las calificaciones
 * @param skipped         filas omitidas porque el libro ya estaba
 * @param errorCount      número de filas con error
 * @param errors          detalle de las primeras filas con error
 */
@Schema(description = "Resultado de importar un CSV")
public record ImportReportResponse(

        @Schema(description = "Formato detectado", example = "GOODREADS")
        String format,

        @Schema(description = "Filas leídas", example = "120")
        int totalRows,

        @Schema(description = "Libros importados", example = "112")
        int importedBooks,

        @Schema(description = "Reseñas creadas", example = "98")
        int importedReviews,

        @Schema(description = "Filas omitidas por duplicado", example = "5")
        int skipped,

        @Schema(description = "Filas con error", example = "3")
        int errorCount,

        @Schema(description = "Detalle de errores")
        List<RowError> errors) {

    /**
     * Error de una fila.
     *
     * @param line    número de registro en el archivo
     * @param message motivo legible
     */
    @Schema(description = "Fila con error")
    public record RowError(

            @Schema(description = "Número de registro", example = "14")
            int line,

            @Schema(description = "Motivo", example = "La fila no tiene título.")
            String message) {
    }
}
