package com.librolog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de arranque del contexto de Spring.
 */
@SpringBootTest
@ActiveProfiles("test")
class LibroLogApplicationTests {

    /**
     * Verifica que el contexto de Spring carga con H2.
     */
    @Test
    void contextLoads() {
    }
}
