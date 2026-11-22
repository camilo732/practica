package com.librolog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Codificador de contraseñas.
 *
 * <p>Vive en su propia configuración para que la cadena de seguridad pueda
 * depender de servicios de autenticación sin crear un ciclo de beans.</p>
 */
@Configuration
public class PasswordConfig {

    /** Coste de BCrypt usado en toda la aplicación. */
    private static final int BCRYPT_COST = 12;

    /**
     * Crea el codificador BCrypt con coste 12.
     *
     * @return codificador de contraseñas
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_COST);
    }
}
