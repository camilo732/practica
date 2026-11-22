package com.librolog.controller;

import com.librolog.dto.request.AddUserBookRequest;
import com.librolog.dto.request.UpdateProgressRequest;
import com.librolog.dto.request.UpdateUserBookRequest;
import com.librolog.dto.response.LibraryStatsResponse;
import com.librolog.dto.response.PageResponse;
import com.librolog.dto.response.UserBookResponse;
import com.librolog.entity.enums.BookStatus;
import com.librolog.security.AuthenticatedUser;
import com.librolog.service.LibraryService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Endpoints de la biblioteca personal.
 *
 * <p>Todas las rutas trabajan sobre el usuario del token: no se acepta un
 * identificador de usuario en la petición, así que no hay forma de consultar la
 * biblioteca de otra persona.</p>
 */
@RestController
@RequestMapping("/api/library")
@Tag(name = "Biblioteca", description = "Biblioteca personal y progreso de lectura")
public class LibraryController {

    private final LibraryService libraryService;

    /**
     * Crea el controlador con sus dependencias.
     *
     * @param libraryService servicio de biblioteca
     */
    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * Añade un libro a la biblioteca personal.
     *
     * @param principal usuario autenticado
     * @param request   datos del alta
     * @return entrada creada y cabecera {@code Location}
     */
    @PostMapping
    @Operation(summary = "Añadir libro",
            description = "Añade un libro a la biblioteca. Se puede indicar un bookId ya cacheado, "
                    + "una clave de Open Library o los datos para un alta manual.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Libro añadido"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "El libro no existe"),
            @ApiResponse(responseCode = "409", description = "El libro ya está en la biblioteca")
    })
    public ResponseEntity<UserBookResponse> add(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AddUserBookRequest request) {

        UserBookResponse created = libraryService.add(principal.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * Lista la biblioteca personal.
     *
     * @param principal usuario autenticado
     * @param status    filtro opcional por estado
     * @param page      página, empezando en 0
     * @param size      tamaño de página
     * @return página de entradas
     */
    @GetMapping
    @Operation(summary = "Listar biblioteca",
            description = "Devuelve la biblioteca personal paginada, de la incorporación más reciente "
                    + "a la más antigua, con filtro opcional por estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Biblioteca devuelta"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public PageResponse<UserBookResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,

            @Parameter(description = "Estado de lectura", example = "READING")
            @RequestParam(name = "status", required = false) BookStatus status,

            @Parameter(description = "Página (desde 0)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (1-100)", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size) {

        return libraryService.list(principal.id(), status, page, size);
    }

    /**
     * Devuelve el resumen de la biblioteca.
     *
     * @param principal usuario autenticado
     * @return conteos por estado
     */
    @GetMapping("/stats")
    @Operation(summary = "Resumen de la biblioteca",
            description = "Cuenta los libros por estado para el panel principal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen devuelto"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public LibraryStatsResponse stats(@AuthenticationPrincipal AuthenticatedUser principal) {
        return libraryService.stats(principal.id());
    }

    /**
     * Devuelve una entrada concreta.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la entrada
     * @return entrada encontrada
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un libro de la biblioteca",
            description = "Devuelve una entrada propia. Si pertenece a otra persona responde 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada devuelta"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "La entrada no existe o no es tuya")
    })
    public UserBookResponse get(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        return libraryService.get(principal.id(), id);
    }

    /**
     * Modifica una entrada de la biblioteca.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la entrada
     * @param request   cambios solicitados
     * @return entrada actualizada
     */
    @PutMapping("/{id}")
    @Operation(summary = "Modificar libro de la biblioteca",
            description = "Cambia el estado, el total de páginas o la fecha de inicio. "
                    + "Las fechas de fin las calcula la base de datos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrada actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "La entrada no existe o no es tuya")
    })
    public UserBookResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserBookRequest request) {
        return libraryService.update(principal.id(), id, request);
    }

    /**
     * Registra el avance de lectura.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la entrada
     * @param request   página actual
     * @return entrada actualizada
     */
    @PatchMapping("/{id}/progress")
    @Operation(summary = "Actualizar progreso",
            description = "Registra la página actual. La base de datos apunta las páginas leídas, "
                    + "cambia a READING y marca READ al llegar al final.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progreso actualizado"),
            @ApiResponse(responseCode = "400", description = "Página inválida"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "La entrada no existe o no es tuya")
    })
    public UserBookResponse updateProgress(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProgressRequest request) {
        return libraryService.updateProgress(principal.id(), id, request);
    }

    /**
     * Elimina una entrada de la biblioteca.
     *
     * @param principal usuario autenticado
     * @param id        identificador de la entrada
     * @return respuesta 204 sin contenido
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar libro de la biblioteca",
            description = "Elimina la entrada y, en cascada, sus reseñas y su bitácora de páginas. "
                    + "El libro permanece en la caché del catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Entrada eliminada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "La entrada no existe o no es tuya")
    })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id) {
        libraryService.delete(principal.id(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
