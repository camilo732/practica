package com.librolog.controller;

import com.librolog.dto.request.ReadingGoalRequest;
import com.librolog.dto.request.UpdateReadingGoalRequest;
import com.librolog.dto.response.ReadingGoalResponse;
import com.librolog.security.AuthenticatedUser;
import com.librolog.service.ReadingGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Endpoints de metas de lectura.
 *
 * <p>Cada respuesta incluye el avance real, el porcentaje esperado a la fecha y
 * el ritmo que hace falta para cumplir la meta.</p>
 */
@RestController
@RequestMapping("/api/goals")
@Tag(name = "Metas", description = "Metas anuales y mensuales con ritmo calculado")
public class ReadingGoalController {

    private final ReadingGoalService goalService;

    /**
     * Crea el controlador con sus dependencias.
     *
     * @param goalService servicio de metas
     */
    public ReadingGoalController(ReadingGoalService goalService) {
        this.goalService = goalService;
    }

    /**
     * Crea una meta de lectura.
     *
     * @param principal usuario autenticado
     * @param request   datos de la meta
     * @return meta creada y cabecera {@code Location}
     */
    @PostMapping
    @Operation(summary = "Crear una meta",
            description = "Crea una meta anual (sin mes) o mensual (con mes). "
                    + "Solo puede existir una meta por periodo y usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Meta creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "409", description = "Ya existe una meta para ese periodo")
    })
    public ResponseEntity<ReadingGoalResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ReadingGoalRequest request) {

        ReadingGoalResponse created = goalService.create(principal.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * Lista las metas del usuario.
     *
     * @param principal usuario autenticado
     * @param year      año por el que filtrar, opcional
     * @return metas con su avance
     */
    @GetMapping
    @Operation(summary = "Listar mis metas",
            description = "Devuelve todas las metas propias con su avance y su ritmo calculado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metas devueltas"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public List<ReadingGoalResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,

            @Parameter(description = "Año a filtrar", example = "2026")
            @RequestParam(name = "year", required = false) Integer year) {
        return goalService.list(principal.id(), year);
    }

    /**
     * Devuelve una meta concreta.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la meta
     * @return meta con su avance
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar una meta",
            description = "Devuelve una meta propia con el avance recalculado en este momento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meta devuelta"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "La meta no existe o no es tuya")
    })
    public ReadingGoalResponse get(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        return goalService.get(principal.id(), id);
    }

    /**
     * Modifica los objetivos de una meta.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la meta
     * @param request   nuevos objetivos
     * @return meta actualizada
     */
    @PutMapping("/{id}")
    @Operation(summary = "Modificar los objetivos de una meta",
            description = "Cambia los libros y páginas objetivo. El periodo no cambia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meta actualizada"),
            @ApiResponse(responseCode = "400", description = "Objetivos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "La meta no existe o no es tuya")
    })
    public ReadingGoalResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReadingGoalRequest request) {
        return goalService.update(principal.id(), id, request);
    }

    /**
     * Elimina una meta.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la meta
     * @return respuesta 204 sin contenido
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una meta",
            description = "Borra una meta propia. No afecta a los libros ni a las reseñas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Meta eliminada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "La meta no existe o no es tuya")
    })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        goalService.delete(principal.id(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
