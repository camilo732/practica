package com.librolog.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Configuración de internacionalización.
 *
 * <p>La API negocia el idioma con la cabecera {@code Accept-Language}, que es la
 * que envía el frontend con el idioma activo. Se admiten español, inglés y
 * alemán; si no llega nada o el idioma no está soportado se usa español.</p>
 */
@Configuration
public class I18nConfig {

    /** Idioma por defecto de la aplicación. */
    public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("es");

    /**
     * Fuente de mensajes de la aplicación.
     *
     * <p>Los archivos están en UTF-8 porque los textos incluyen tildes y
     * eñes.</p>
     *
     * @return fuente de mensajes basada en {@code messages*.properties}
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);
        return source;
    }

    /**
     * Resuelve el idioma a partir de {@code Accept-Language}.
     *
     * @return resolutor con español como idioma por defecto
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(
                DEFAULT_LOCALE,
                Locale.forLanguageTag("en"),
                Locale.forLanguageTag("de")));
        resolver.setDefaultLocale(DEFAULT_LOCALE);
        return resolver;
    }
}
