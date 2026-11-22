package com.librolog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración para servir el frontend estático durante el desarrollo.
 *
 * <p>En producción, Maven copia {@code frontend/} a {@code static/} y Spring
 * Boot lo sirve desde el classpath. En desarrollo e IntelliJ también se sirve
 * directamente la carpeta {@code ../frontend} para no depender de una copia.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String frontendDir;

    /**
     * Crea la configuración con la ruta del frontend.
     *
     * @param frontendDir ruta de la carpeta del frontend
     */
    public WebConfig(@Value("${app.frontend-dir:../frontend}") String frontendDir) {
        this.frontendDir = frontendDir;
    }

    /**
     * Registra la carpeta del frontend como recurso estático si existe.
     *
     * @param registry registro de manejadores de recursos
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(frontendDir).toAbsolutePath().normalize();
        if (Files.isDirectory(path)) {
            registry.addResourceHandler("/**")
                    .addResourceLocations(path.toUri().toString())
                    .setCachePeriod(0);
        }
    }
}
