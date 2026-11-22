package com.librolog.service;

import com.librolog.dto.response.AdminAuditResponse;
import com.librolog.dto.response.AdminStatsResponse;
import com.librolog.dto.response.AdminUserResponse;
import com.librolog.dto.response.PageResponse;
import com.librolog.entity.AdminAuditLog;
import com.librolog.entity.Role;
import com.librolog.entity.User;
import com.librolog.entity.enums.AdminAuditAction;
import com.librolog.entity.enums.AuthProvider;
import com.librolog.entity.enums.RoleName;
import com.librolog.exception.ConflictException;
import com.librolog.repository.AdminAuditLogRepository;
import com.librolog.repository.RefreshTokenRepository;
import com.librolog.repository.RoleRepository;
import com.librolog.repository.UserRepository;
import com.librolog.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Gestión de usuarios desde el panel de administración.
 *
 * <p>Protege las operaciones que podrían dejar el sistema sin administradores:
 * nadie puede deshabilitarse ni retirarse el rol a sí mismo, y siempre debe
 * quedar al menos una cuenta administradora activa. Cada cambio queda en la
 * auditoría.</p>
 */
@Service
public class AdminService {

    /** Tamaño máximo de página. */
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AdminAuditLogRepository auditRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository        repositorio de usuarios
     * @param roleRepository        repositorio de roles
     * @param refreshTokenRepository repositorio de sesiones
     * @param auditRepository       repositorio de auditoría
     */
    public AdminService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        AdminAuditLogRepository auditRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditRepository = auditRepository;
    }

    /**
     * Devuelve el resumen de cuentas.
     *
     * @return estadísticas del panel
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        Instant now = Instant.now();
        Instant startOfMonth = YearMonth.now(ZoneOffset.UTC)
                .atDay(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        long total = userRepository.count();
        long enabled = userRepository.countByEnabledTrue();
        long admins = userRepository.countByRoles_Name(RoleName.ADMIN);
        long locked = userRepository.countByLockedUntilAfter(now);
        long google = userRepository.countByAuthProvider(AuthProvider.GOOGLE);
        long local = userRepository.countByAuthProvider(AuthProvider.LOCAL);
        long newThisMonth = userRepository.countByCreatedAtAfter(startOfMonth);

        return new AdminStatsResponse(
                total,
                enabled,
                total - enabled,
                admins,
                locked,
                google,
                local,
                newThisMonth);
    }

    /**
     * Lista cuentas con filtros opcionales.
     *
     * @param query  texto libre para usuario, correo o nombre
     * @param role   rol requerido: {@code USER}, {@code ADMIN} o vacío
     * @param status estado: {@code enabled}, {@code disabled} o vacío
     * @param page   página, empezando en 0
     * @param size   tamaño de página
     * @return página de cuentas
     * @throws IllegalArgumentException si el rol o el estado no son válidos
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(String query, String role,
                                                     String status, int page, int size) {
        RoleName roleFilter = parseRole(role);
        Boolean enabledFilter = parseStatus(status);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(MAX_PAGE_SIZE, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        String q = query == null || query.isBlank() ? "" : query.trim();
        String roleValue = roleFilter == null ? "" : roleFilter.name();

        Page<User> result = userRepository.searchForAdmin(q, enabledFilter, roleValue, pageable);
        return PageResponse.from(result.map(AdminUserResponse::from));
    }

    /**
     * Devuelve una cuenta concreta.
     *
     * @param id identificador
     * @return cuenta
     * @throws NoSuchElementException si no existe
     */
    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        return AdminUserResponse.from(findUser(id));
    }

    /**
     * Habilita o deshabilita una cuenta.
     *
     * <p>Al deshabilitarla se cierran todas sus sesiones. No se permite
     * deshabilitar la propia cuenta ni dejar el sistema sin administradores.</p>
     *
     * @param actor   administrador que realiza el cambio
     * @param userId  cuenta afectada
     * @param enabled nuevo estado
     * @return cuenta actualizada
     * @throws NoSuchElementException si la cuenta no existe
     * @throws ConflictException      si la operación dejaría el sistema sin acceso
     */
    @Transactional
    public AdminUserResponse setEnabled(AuthenticatedUser actor, Long userId, boolean enabled) {
        User target = findUser(userId);

        if (target.getId().equals(actor.id()) && !enabled) {
            throw new ConflictException("No puedes deshabilitar tu propia cuenta.");
        }
        if (!enabled && target.isEnabled() && isAdmin(target) && activeAdminCount() <= 1) {
            throw new ConflictException("Debe quedar al menos un administrador activo.");
        }

        boolean changed = target.isEnabled() != enabled;
        target.setEnabled(enabled);

        if (!enabled) {
            refreshTokenRepository.revokeAllByUserId(target.getId(), Instant.now());
        }
        if (changed) {
            audit(actor, target,
                    enabled ? AdminAuditAction.USER_ENABLED : AdminAuditAction.USER_DISABLED,
                    null);
        }

        return AdminUserResponse.from(target);
    }

    /**
     * Concede o retira el rol de administrador.
     *
     * <p>No se permite retirar el rol a la propia cuenta ni dejar el sistema sin
     * administradores activos. Al retirarlo se cierran las sesiones de la
     * cuenta afectada.</p>
     *
     * @param actor  administrador que realiza el cambio
     * @param userId cuenta afectada
     * @param admin  {@code true} para conceder, {@code false} para retirar
     * @return cuenta actualizada
     * @throws NoSuchElementException si la cuenta no existe
     * @throws ConflictException      si la operación dejaría el sistema sin acceso
     */
    @Transactional
    public AdminUserResponse setAdmin(AuthenticatedUser actor, Long userId, boolean admin) {
        User target = findUser(userId);
        boolean currentlyAdmin = isAdmin(target);

        if (currentlyAdmin == admin) {
            return AdminUserResponse.from(target);
        }
        if (!admin && target.getId().equals(actor.id())) {
            throw new ConflictException("No puedes retirarte el rol de administrador.");
        }
        if (!admin && currentlyAdmin && target.isEnabled() && activeAdminCount() <= 1) {
            throw new ConflictException("Debe quedar al menos un administrador activo.");
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el rol ADMIN en la base de datos. Ejecuta las migraciones de Flyway."));

        if (admin) {
            target.addRole(adminRole);
            roleRepository.findByName(RoleName.USER).ifPresent(target::addRole);
        } else {
            target.getRoles().removeIf(role -> role.getName() == RoleName.ADMIN);
            if (target.getRoles().isEmpty()) {
                roleRepository.findByName(RoleName.USER).ifPresent(target::addRole);
            }
            refreshTokenRepository.revokeAllByUserId(target.getId(), Instant.now());
        }

        audit(actor, target,
                admin ? AdminAuditAction.ROLE_GRANTED : AdminAuditAction.ROLE_REVOKED,
                null);

        return AdminUserResponse.from(target);
    }

    /**
     * Desbloquea una cuenta y reinicia sus intentos fallidos.
     *
     * @param actor  administrador que realiza el cambio
     * @param userId cuenta afectada
     * @return cuenta actualizada
     * @throws NoSuchElementException si la cuenta no existe
     */
    @Transactional
    public AdminUserResponse unlock(AuthenticatedUser actor, Long userId) {
        User target = findUser(userId);
        boolean changed = target.getLockedUntil() != null || target.getFailedAttempts() != 0;

        target.setFailedAttempts((short) 0);
        target.setLockedUntil(null);

        if (changed) {
            audit(actor, target, AdminAuditAction.USER_UNLOCKED, null);
        }

        return AdminUserResponse.from(target);
    }

    /**
     * Cierra todas las sesiones de una cuenta.
     *
     * @param actor  administrador que realiza el cambio
     * @param userId cuenta afectada
     * @return número de sesiones revocadas
     * @throws NoSuchElementException si la cuenta no existe
     */
    @Transactional
    public int revokeSessions(AuthenticatedUser actor, Long userId) {
        User target = findUser(userId);
        int revoked = refreshTokenRepository.revokeAllByUserId(target.getId(), Instant.now());
        audit(actor, target, AdminAuditAction.SESSIONS_REVOKED, String.valueOf(revoked));
        return revoked;
    }

    /**
     * Lista la auditoría administrativa.
     *
     * @param page página, empezando en 0
     * @param size tamaño de página
     * @return página de registros
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminAuditResponse> listAudit(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(MAX_PAGE_SIZE, size)));

        Page<AdminAuditLog> result = auditRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(result.map(AdminAuditResponse::from));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe esa cuenta."));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
    }

    private long activeAdminCount() {
        return userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN);
    }

    private void audit(AuthenticatedUser actor, User target, AdminAuditAction action, String detail) {
        AdminAuditLog log = new AdminAuditLog();
        log.setActor(userRepository.getReferenceById(actor.id()));
        log.setActorUsername(actor.username());
        log.setTargetUser(target);
        log.setTargetUsername(target.getUsername());
        log.setAction(action);
        log.setDetail(detail);
        auditRepository.save(log);
    }

    private static RoleName parseRole(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return RoleName.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol no válido. Usa USER o ADMIN.");
        }
    }

    private static Boolean parseStatus(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "enabled" -> true;
            case "disabled" -> false;
            default -> throw new IllegalArgumentException(
                    "Estado no válido. Usa enabled o disabled.");
        };
    }
}
