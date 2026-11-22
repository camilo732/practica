package com.librolog.service;

import com.librolog.entity.AdminAuditLog;
import com.librolog.entity.Role;
import com.librolog.entity.User;
import com.librolog.entity.enums.RoleName;
import com.librolog.exception.ConflictException;
import com.librolog.repository.AdminAuditLogRepository;
import com.librolog.repository.RefreshTokenRepository;
import com.librolog.repository.RoleRepository;
import com.librolog.repository.UserRepository;
import com.librolog.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las reglas de administración de usuarios.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AdminAuditLogRepository auditRepository;

    private AdminService service;
    private AuthenticatedUser actor;
    private Role userRole;
    private Role adminRole;

    /**
     * Prepara el servicio, el actor y los roles.
     */
    @BeforeEach
    void setUp() {
        service = new AdminService(userRepository, roleRepository,
                refreshTokenRepository, auditRepository);
        actor = new AuthenticatedUser(1L, "admin", Set.of("ADMIN"));
        userRole = new Role();
        userRole.setName(RoleName.USER);
        adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
    }

    /**
     * Deshabilita una cuenta y revoca sus sesiones.
     */
    @Test
    void disablesUserAndRevokesSessions() {
        User target = user(2L, "ana", true, userRole);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(refreshTokenRepository.revokeAllByUserId(eq(2L), any(Instant.class))).thenReturn(2);

        var result = service.setEnabled(actor, 2L, false);

        assertFalse(result.enabled());
        verify(refreshTokenRepository).revokeAllByUserId(eq(2L), any(Instant.class));
        verify(auditRepository).save(any(AdminAuditLog.class));
    }

    /**
     * No permite deshabilitar la propia cuenta.
     */
    @Test
    void rejectsDisablingSelf() {
        User target = user(1L, "admin", true, adminRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        assertThrows(ConflictException.class, () -> service.setEnabled(actor, 1L, false));
    }

    /**
     * No permite deshabilitar al último administrador activo.
     */
    @Test
    void rejectsDisablingLastAdmin() {
        User target = user(2L, "otro_admin", true, adminRole);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.setEnabled(actor, 2L, false));
    }

    /**
     * No permite retirar el rol a la propia cuenta.
     */
    @Test
    void rejectsRemovingOwnAdminRole() {
        User target = user(1L, "admin", true, adminRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        assertThrows(ConflictException.class, () -> service.setAdmin(actor, 1L, false));
    }

    /**
     * Desbloquea una cuenta y reinicia los intentos fallidos.
     */
    @Test
    void unlocksAccount() {
        User target = user(3L, "ana", true, userRole);
        target.setFailedAttempts((short) 4);
        target.setLockedUntil(Instant.now().plusSeconds(600));
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));

        var result = service.unlock(actor, 3L);

        assertEquals(0, result.failedAttempts());
        assertNull(result.lockedUntil());
        assertFalse(result.locked());
    }

    private static User user(Long id, String username, boolean enabled, Role... roles) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setEnabled(enabled);
        for (Role role : roles) {
            user.addRole(role);
        }
        return user;
    }
}
