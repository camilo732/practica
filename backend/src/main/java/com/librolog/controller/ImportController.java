package com.librolog.controller;

import com.librolog.dto.response.ImportReportResponse;
import com.librolog.security.AuthenticatedUser;
import com.librolog.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint de importación de bibliotecas desde CSV.
 */
@RestController
@RequestMapping("/api/import")
@Tag(name = "Importación", description = "Importación de bibliotecas desde CSV")
public class ImportController {

    private final ImportService importService;

    /**
     * Crea el controlador con el servicio de importación.
     *
     * @param importService servicio de importación
     */
    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    /**
     * Importa un CSV de LibroLog o de Goodreads.
     *
     * @param principal usuario autenticado
     * @param file      archivo CSV
     * @return informe de la importación
     */
    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importar biblioteca desde CSV",
            description = "Acepta el CSV exportado por LibroLog y el de Goodreads. "
                    + "Los libros repetidos se omiten, las filas con error se informan "
                    + "y las calificaciones se convierten en reseñas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Importación terminada"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío o formato no reconocido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "413", description = "El archivo supera el máximo de 10 MB")
    })
    public ImportReportResponse importCsv(
            @AuthenticationPrincipal AuthenticatedUser principal,

            @Parameter(description = "Archivo CSV")
            @RequestPart("file") MultipartFile file) {
        return importService.importCsv(principal.id(), file);
    }
}
