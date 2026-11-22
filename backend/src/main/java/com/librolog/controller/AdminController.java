package com.librolog.controller;

import com.librolog.dto.request.UpdateUserRoleRequest;
import com.librolog.dto.request.UpdateUserStatusRequest;
import com.librolog.dto.response.AdminAuditResponse;
import com.librolog.dto.response.AdminStatsResponse;
import com.librolog.dto.response.AdminUsageResponse;
import com.librolog.dto.response.AdminUserResponse;
import com.librolog.dto.response.PageResponse;
import com.librolog.security.AuthenticatedUser;
import com.librolog.service.AdminService;
import com.librolog.service.AdminUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de administración de usuarios.
 *
 * <p>Todas las rutas exigen el rol {@code ADMIN}. Las operaciones sensibles
 * están protegidas en el servicio para no dejar el sistema sin
 * administradores.</p>
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administración", description = "Gestión de cuentas, roles y auditoría")
public class AdminController {

    private final AdminService adminService;
    private final AdminUsageService adminUsageService;

    /**
     * Crea el controlador con sus dependencias.
     *
     * @param adminService      servicio de administración
     * @param adminUsageService servicio de reportes globales de uso
     */
    public AdminController(AdminService adminService, AdminUsageService adminUsageService) {
        this.adminService = adminService;
        this.adminUsageService = adminUsageService;
    }

    /**
     * Devuelve el resumen de cuentas.
     *
     * @return estadísticas del panel
     */
    @GetMapping("/stats")
    @Operation(summary = "Resumen de cuentas",
            description = "Devuelve totales de cuentas, administradores, bloqueos y proveedores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen devuelto"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    })
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    /**
     * Devuelve el reporte global de uso de la plataforma.
     *
     * @return indicadores agregados, sin contenido privado
     */
    @GetMapping("/usage")
    @Operation(summary = "Reporte global de uso",
            description = "Libros registrados, reseñas, adopción de metas y actividad del mes. "
                    + "Solo datos agregados: no expone bibliotecas ni reseñas de nadie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte devuelto"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    })
    public AdminUsageResponse usage() {
        return adminUsageService.usage();
    }

    /**
     * Lista las cuentas con filtros opcionales.
     *
     * @param query  texto libre para usuario, correo o nombre
     * @param role   rol requerido: USER, ADMIN o vacío
     * @param status estado: enabled, disabled o vacío
     * @param page   página, empezando en 0
     * @param size   tamaño de página
     * @return página de cuentas
     */
    @GetMapping("/users")
    @Operation(summary = "Listar cuentas",
            description = "Lista las cuentas registradas con búsqueda, filtro por rol y estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuentas devueltas"),
            @ApiResponse(responseCode = "400", description = "Filtro no válido"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    })
    public PageResponse<AdminUserResponse> listUsers(
            @Parameter(description = "Texto para buscar por usuario, correo o nombre")
            @RequestParam(name = "q", required = false) String query,

            @Parameter(description = "Rol: USER, ADMIN o vacío", example = "ADMIN")
            @RequestParam(name = "role", required = false) String role,

            @Parameter(description = "Estado: enabled, disabled o vacío", example = "enabled")
            @RequestParam(name = "status", required = false) String status,

            @Parameter(description = "Página (desde 0)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (1-100)", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size) {

        return adminService.listUsers(query, role, status, page, size);
    }

    /**
     * Devuelve una cuenta concreta.
     *
     * @param id identificador
     * @return cuenta
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Consultar una cuenta",
            description = "Devuelve los datos administrativos de una cuenta concreta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta devuelta"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe")
    })
    public AdminUserResponse getUser(@PathVariable Long id) {
        return adminService.getUser(id);
    }

    /**
     * Habilita o deshabilita una cuenta.
     *
     * @param actor   administrador autenticado
     * @param id      cuenta afectada
     * @param request nuevo estado
     * @return cuenta actualizada
     */
    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Cambiar el estado de una cuenta",
            description = "Habilita o deshabilita una cuenta. Al deshabilitarla se cierran sus sesiones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe"),
            @ApiResponse(responseCode = "409", description = "La operación dejaría el sistema sin administradores")
    })
    public AdminUserResponse updateStatus(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        return adminService.setEnabled(actor, id, request.enabled());
    }

    /**
     * Concede o retira el rol de administrador.
     *
     * @param actor   administrador autenticado
     * @param id      cuenta afectada
     * @param request cambio de rol
     * @return cuenta actualizada
     */
    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Cambiar el rol de administrador",
            description = "Concede o retira ADMIN. No se permite retirar el rol a la propia cuenta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe"),
            @ApiResponse(responseCode = "409", description = "La operación dejaría el sistema sin administradores")
    })
    public AdminUserResponse updateRole(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        return adminService.setAdmin(actor, id, request.admin());
    }

    /**
     * Desbloquea una cuenta.
     *
     * @param actor administrador autenticado
     * @param id    cuenta afectada
     * @return cuenta actualizada
     */
    @PostMapping("/users/{id}/unlock")
    @Operation(summary = "Desbloquear una cuenta",
            description = "Reinicia los intentos fallidos y elimina el bloqueo temporal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cuenta desbloqueada"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe")
    })
    public AdminUserResponse unlock(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long id) {

        return adminService.unlock(actor, id);
    }

    /**
     * Cierra todas las sesiones de una cuenta.
     *
     * @param actor administrador autenticado
     * @param id    cuenta afectada
     * @return respuesta 204 sin contenido
     */
    @PostMapping("/users/{id}/sessions/revoke")
    @Operation(summary = "Cerrar las sesiones de una cuenta",
            description = "Revoca todos los refresh tokens de la cuenta afectada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesiones revocadas"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN"),
            @ApiResponse(responseCode = "404", description = "La cuenta no existe")
    })
    public ResponseEntity<Void> revokeSessions(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable Long id) {

        adminService.revokeSessions(actor, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lista la auditoría administrativa.
     *
     * @param page página, empezando en 0
     * @param size tamaño de página
     * @return página de registros
     */
    @GetMapping("/audit")
    @Operation(summary = "Listar la auditoría",
            description = "Devuelve las acciones administrativas de la más reciente a la más antigua.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Auditoría devuelta"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    })
    public PageResponse<AdminAuditResponse> audit(
            @Parameter(description = "Página (desde 0)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (1-100)", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size) {

        return adminService.listAudit(page, size);
    }
}
