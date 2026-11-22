package com.librolog.controller;

import com.librolog.dto.response.AppInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de LibroLog.
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "Público", description = "Endpoints abiertos sin autenticación")
public class PublicController {

    /**
     * Devuelve información básica de la API.
     *
     * @return nombre, versión, estado y ruta de la documentación
     */
    @GetMapping("/info")
    @Operation(
            summary = "Información de la API",
            description = "Devuelve el nombre, la versión y el estado de LibroLog.")
    @ApiResponse(responseCode = "200", description = "Información devuelta correctamente")
    public AppInfoResponse info() {
        return new AppInfoResponse(
                "LibroLog",
                "1.0.0",
                "UP",
                "/swagger-ui.html");
    }
}
