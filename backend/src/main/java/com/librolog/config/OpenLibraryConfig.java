package com.librolog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuración del cliente HTTP de Open Library.
 *
 * <p>Los tiempos de espera son cortos a propósito: si la API pública no
 * responde, la búsqueda debe degradar a la caché local en lugar de dejar a la
 * persona usuaria esperando.</p>
 */
@Configuration
@EnableConfigurationProperties(OpenLibraryProperties.class)
public class OpenLibraryConfig {

    /**
     * Crea el cliente HTTP con tiempos de espera y cabeceras identificativas.
     *
     * @param properties configuración de Open Library
     * @return cliente listo para usar
     */
    @Bean
    public RestClient openLibraryRestClient(OpenLibraryProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
