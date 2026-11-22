package com.librolog.service;

import com.librolog.entity.Role;
import com.librolog.entity.User;
import com.librolog.entity.enums.AuthProvider;
import com.librolog.entity.enums.RoleName;
import com.librolog.exception.OAuthLoginException;
import com.librolog.repository.RoleRepository;
import com.librolog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Pruebas de vinculación de cuentas de Google.
 */
@ExtendWith(MockitoExtension.class)
class GoogleAccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private GoogleAccountService service;
    private Role userRole;

    /**
     * Prepara el servicio y el rol USER.
     */
    @BeforeEach
    void setUp() {
        service = new GoogleAccountService(userRepository, roleRepository);
        userRole = new Role();
        userRole.setName(RoleName.USER);
    }

    /**
     * Crea una cuenta nueva con idioma, proveedor y rol correctos.
     */
    @Test
    void createsGoogleUser() {
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("juan@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.resolve(new GoogleAccount(
                "google-1", "Juan@Example.com", true, "Juan Pérez", "de-DE"));

        assertEquals("juan", user.getUsername());
        assertEquals("juan@example.com", user.getEmail());
        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
        assertEquals("google-1", user.getProviderId());
        assertEquals("de", user.getLanguage());
        assertNull(user.getPassword());
        assertTrue(user.getRoles().contains(userRole));
    }

    /**
     * Vincula una cuenta local existente sin borrar la contraseña.
     */
    @Test
    void linksExistingLocalAccount() {
        User local = new User();
        local.setId(7L);
        local.setUsername("juan");
        local.setEmail("juan@example.com");
        local.setPassword("hash-bcrypt");
        local.setAuthProvider(AuthProvider.LOCAL);
        local.addRole(userRole);

        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("juan@example.com"))
                .thenReturn(Optional.of(local));

        User user = service.resolve(new GoogleAccount(
                "google-1", "juan@example.com", true, "Juan", "es"));

        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
        assertEquals("google-1", user.getProviderId());
        assertEquals("hash-bcrypt", user.getPassword());
    }

    /**
     * Rechaza correos que Google no ha verificado.
     */
    @Test
    void rejectsUnverifiedEmail() {
        assertThrows(OAuthLoginException.class, () -> service.resolve(new GoogleAccount(
                "google-1", "juan@example.com", false, "Juan", "es")));
    }

    /**
     * Rechaza cuentas deshabilitadas.
     */
    @Test
    void rejectsDisabledAccount() {
        User disabled = new User();
        disabled.setId(9L);
        disabled.setUsername("juan");
        disabled.setEmail("juan@example.com");
        disabled.setEnabled(false);
        disabled.setAuthProvider(AuthProvider.GOOGLE);
        disabled.setProviderId("google-1");

        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google-1"))
                .thenReturn(Optional.of(disabled));

        assertThrows(OAuthLoginException.class, () -> service.resolve(new GoogleAccount(
                "google-1", "juan@example.com", true, "Juan", "es")));
    }

    /**
     * Genera un nombre de usuario alternativo cuando ya existe.
     */
    @Test
    void generatesUniqueUsername() {
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "google-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("juan@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase("juan")).thenReturn(true);
        when(userRepository.existsByUsernameIgnoreCase("juan_2")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.resolve(new GoogleAccount(
                "google-1", "juan@example.com", true, null, null));

        assertEquals("juan_2", user.getUsername());
        assertEquals("es", user.getLanguage());
    }
}
