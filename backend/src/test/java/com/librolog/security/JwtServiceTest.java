package com.librolog.security;

import com.librolog.config.JwtProperties;
import com.librolog.entity.Role;
import com.librolog.entity.User;
import com.librolog.entity.enums.RoleName;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del servicio de tokens JWT.
 */
class JwtServiceTest {

    private static final String SECRETO =
            "secreto-de-prueba-con-mas-de-treinta-y-dos-caracteres-2026";

    private JwtService servicio() {
        return new JwtService(new JwtProperties(SECRETO, 15, 7));
    }

    private User usuario() {
        User user = new User();
        user.setId(42L);
        user.setUsername("ana_lectora");

        Role role = new Role();
        role.setName(RoleName.ADMIN);
        user.addRole(role);
        return user;
    }

    /**
     * Comprueba que un token emitido se puede leer con los mismos datos.
     */
    @Test
    void emiteYValidaUnToken() {
        JwtService service = servicio();

        String token = service.generateAccessToken(usuario());
        JwtPayload payload = service.parse(token);

        assertEquals(42L, payload.userId());
        assertEquals("ana_lectora", payload.username());
        assertTrue(payload.roles().contains("ADMIN"));
        assertEquals(900L, service.accessTokenSeconds());
    }

    /**
     * Comprueba que una firma manipulada se rechaza.
     */
    @Test
    void rechazaTokenManipulado() {
        JwtService service = servicio();
        String token = service.generateAccessToken(usuario());
        String manipulado = token.substring(0, token.length() - 2) + "xy";

        assertThrows(JwtException.class, () -> service.parse(manipulado));
    }

    /**
     * Comprueba que un secreto demasiado corto impide arrancar el servicio.
     */
    @Test
    void rechazaSecretoCorto() {
        JwtProperties properties = new JwtProperties("corto", 15, 7);

        assertThrows(IllegalStateException.class, () -> new JwtService(properties));
    }
}
