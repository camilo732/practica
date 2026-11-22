package com.librolog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reporte global de uso de la plataforma.
 *
 * <p>Solo contiene conteos agregados: no expone títulos, reseñas ni ningún
 * contenido de la biblioteca de una persona concreta.</p>
 *
 * @param year                  año usado para las metas
 * @param libraryEntries        libros registrados en todas las bibliotecas
 * @param finishedBooks         libros marcados como terminados
 * @param averageBooksPerUser   libros registrados por cuenta, con un decimal
 * @param reviews               reseñas escritas en total
 * @param usersWithGoal         cuentas con al menos una meta en el año
 * @param goalAdoptionPercent   porcentaje de cuentas con meta en el año
 * @param activeUsersThisMonth  cuentas que registraron progreso este mes
 * @param pagesLoggedThisMonth  páginas registradas este mes en toda la plataforma
 */
@Schema(description = "Reporte global de uso, solo con datos agregados")
public record AdminUsageResponse(

        @Schema(description = "Año de las metas", example = "2026")
        int year,

        @Schema(description = "Libros registrados en todas las bibliotecas", example = "320")
        long libraryEntries,

        @Schema(description = "Libros terminados", example = "140")
        long finishedBooks,

        @Schema(description = "Libros registrados por cuenta", example = "13.3")
        double averageBooksPerUser,

        @Schema(description = "Reseñas escritas", example = "85")
        long reviews,

        @Schema(description = "Cuentas con meta en el año", example = "15")
        long usersWithGoal,

        @Schema(description = "Porcentaje de cuentas con meta", example = "62.5")
        double goalAdoptionPercent,

        @Schema(description = "Cuentas activas este mes", example = "9")
        long activeUsersThisMonth,

        @Schema(description = "Páginas registradas este mes", example = "4200")
        long pagesLoggedThisMonth) {
}
