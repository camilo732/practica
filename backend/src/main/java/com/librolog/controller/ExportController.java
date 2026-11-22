package com.librolog.controller;

import com.librolog.dto.response.LibraryExportResponse;
import com.librolog.security.AuthenticatedUser;
import com.librolog.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de exportación de la biblioteca.
 *
 * <p>El CSV sirve para hojas de cálculo y para reimportar; el JSON es una copia
 * de seguridad completa con reseñas y metas.</p>
 */
@RestController
@RequestMapping("/api/export")
@Tag(name = "Exportación", description = "Descarga de la biblioteca en CSV y JSON")
public class ExportController {

    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final ExportService exportService;

    /**
     * Crea el controlador con el servicio de exportación.
     *
     * @param exportService servicio de exportación
     */
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Descarga la biblioteca en CSV.
     *
     * @param principal usuario autenticado
     * @return archivo CSV como adjunto
     */
    @GetMapping("/csv")
    @Operation(summary = "Exportar biblioteca en CSV",
            description = "Genera un CSV con una fila por libro, compatible con la "
                    + "importación y con hojas de cálculo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV generado"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public ResponseEntity<byte[]> csv(@AuthenticationPrincipal AuthenticatedUser principal) {
        byte[] content = exportService.csv(principal.id());

        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("librolog-biblioteca.csv")
                                .build()
                                .toString())
                .body(content);
    }

    /**
     * Descarga la biblioteca completa en JSON.
     *
     * @param principal usuario autenticado
     * @return documento JSON como adjunto
     */
    @GetMapping("/json")
    @Operation(summary = "Exportar biblioteca en JSON",
            description = "Genera una copia de seguridad con libros, reseñas y metas. "
                    + "No incluye credenciales ni datos de otras personas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JSON generado"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public ResponseEntity<LibraryExportResponse> json(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        LibraryExportResponse export = exportService.json(principal.id());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("librolog-export.json")
                                .build()
                                .toString())
                .body(export);
    }
}
