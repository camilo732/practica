package com.librolog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para LibroLog.
 *
 * <p>Define el título, la descripción, la versión y el esquema Bearer JWT que
 * aparece en el botón <em>Authorize</em> de Swagger UI.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    /**
     * Construye el documento OpenAPI principal.
     *
     * @return documento OpenAPI con información y seguridad JWT
     */
    @Bean
    public OpenAPI libroLogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LibroLog API")
                        .description("""
                                API REST de LibroLog: biblioteca personal, reseñas,
                                metas de lectura, reportes, importación/exportación
                                y administración de usuarios.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Proyecto LibroLog")
                                .email("contacto@librolog.local"))
                        .license(new License()
                                .name("Uso académico")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT de acceso")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
