package com.librolog.controller;

import com.librolog.dto.request.ReviewRequest;
import com.librolog.dto.response.PageResponse;
import com.librolog.dto.response.ReviewResponse;
import com.librolog.security.AuthenticatedUser;
import com.librolog.service.ReviewService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de reseñas estructuradas.
 *
 * <p>La reseña cuelga de la entrada de biblioteca, no del libro: dos personas
 * pueden reseñar la misma obra con valoraciones distintas.</p>
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Reseñas", description = "Reseñas con calificación general y subcalificaciones")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Crea el controlador con sus dependencias.
     *
     * @param reviewService servicio de reseñas
     */
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Crea o reemplaza la reseña de una entrada de biblioteca.
     *
     * @param principal  usuario autenticado
     * @param userBookId entrada de biblioteca
     * @param request    calificaciones y texto
     * @return reseña guardada
     */
    @PutMapping("/library/{userBookId}/review")
    @Operation(summary = "Crear o modificar una reseña",
            description = "Cada libro de la biblioteca admite una sola reseña. "
                    + "El alta exige que el libro esté terminado; después se puede editar siempre.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reseña guardada"),
            @ApiResponse(responseCode = "400", description = "Calificaciones fuera de rango"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "La entrada no existe o no es tuya"),
            @ApiResponse(responseCode = "409", description = "El libro aún no está terminado")
    })
    public ReviewResponse upsert(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long userBookId,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.upsert(principal.id(), userBookId, request);
    }

    /**
     * Devuelve la reseña de una entrada propia.
     *
     * @param principal  usuario autenticado
     * @param userBookId entrada de biblioteca
     * @return reseña encontrada
     */
    @GetMapping("/library/{userBookId}/review")
    @Operation(summary = "Consultar la reseña de un libro",
            description = "Devuelve la reseña de una entrada propia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reseña devuelta"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "No hay reseña o la entrada no es tuya")
    })
    public ReviewResponse get(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long userBookId) {
        return reviewService.get(principal.id(), userBookId);
    }

    /**
     * Elimina la reseña de una entrada propia.
     *
     * @param principal  usuario autenticado
     * @param userBookId entrada de biblioteca
     * @return respuesta 204 sin contenido
     */
    @DeleteMapping("/library/{userBookId}/review")
    @Operation(summary = "Eliminar una reseña",
            description = "Borra la reseña sin sacar el libro de la biblioteca.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reseña eliminada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "No hay reseña o la entrada no es tuya")
    })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long userBookId) {
        reviewService.delete(principal.id(), userBookId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lista las reseñas del usuario.
     *
     * @param principal usuario autenticado
     * @param page      página, empezando en 0
     * @param size      tamaño de página
     * @return página de reseñas
     */
    @GetMapping("/reviews")
    @Operation(summary = "Listar mis reseñas",
            description = "Devuelve las reseñas propias de la más reciente a la más antigua.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reseñas devueltas"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public PageResponse<ReviewResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,

            @Parameter(description = "Página (desde 0)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (1-100)", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return reviewService.list(principal.id(), page, size);
    }
}
