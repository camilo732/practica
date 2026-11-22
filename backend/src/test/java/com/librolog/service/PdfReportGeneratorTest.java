package com.librolog.service;

import com.librolog.dto.response.AnalyticsSummaryResponse;
import com.librolog.dto.response.AuthorCountResponse;
import com.librolog.dto.response.MonthlyActivityResponse;
import com.librolog.dto.response.RatingCountResponse;
import com.librolog.entity.enums.GoalPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del informe PDF en los tres idiomas.
 */
class PdfReportGeneratorTest {

    private PdfReportGenerator generator;

    /**
     * Prepara la fuente de mensajes real de la aplicación.
     */
    @BeforeEach
    void setUp() {
        ReloadableResourceBundleMessageSource source =
                new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        generator = new PdfReportGenerator(source);
    }

    /**
     * Comprueba que se genera un PDF en español.
     */
    @Test
    void generaElInformeEnEspanol() {
        byte[] pdf = generator.render(summary(), goals(), "ana_lectora",
                Locale.forLanguageTag("es"));

        assertPdf(pdf);
        assertEquals("informe-librolog-2026.pdf",
                generator.filename(2026, Locale.forLanguageTag("es")));
    }

    /**
     * Comprueba que se genera un PDF en inglés.
     */
    @Test
    void generaElInformeEnIngles() {
        byte[] pdf = generator.render(summary(), goals(), "ana_lectora",
                Locale.forLanguageTag("en"));

        assertPdf(pdf);
        assertEquals("librolog-report-2026.pdf",
                generator.filename(2026, Locale.forLanguageTag("en")));
    }

    /**
     * Comprueba que se genera un PDF en alemán.
     */
    @Test
    void generaElInformeEnAleman() {
        byte[] pdf = generator.render(summary(), goals(), "ana_lectora",
                Locale.forLanguageTag("de"));

        assertPdf(pdf);
        assertEquals("librolog-bericht-2026.pdf",
                generator.filename(2026, Locale.forLanguageTag("de")));
    }

    /**
     * Comprueba que un usuario sin metas ni autores sigue generando el informe.
     */
    @Test
    void generaElInformeSinDatos() {
        AnalyticsSummaryResponse empty = new AnalyticsSummaryResponse(
                2026, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0.0, 0, 0,
                months(), List.of(), List.of());

        byte[] pdf = generator.render(empty, List.of(), "nuevo", Locale.forLanguageTag("es"));

        assertPdf(pdf);
    }

    private static void assertPdf(byte[] pdf) {
        assertTrue(pdf.length > 800, "El PDF es demasiado pequeño: " + pdf.length);
        String magic = new String(pdf, 0, 4, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF", magic);
    }

    /**
     * Comprueba que el texto del PDF conserva las tildes y las secciones.
     *
     * @throws IOException si el PDF generado no se puede releer
     */
    @Test
    void extraeElTextoEnEspanolConTildes() throws IOException {
        byte[] pdf = generator.render(summary(), goals(), "ana_lectora",
                Locale.forLanguageTag("es"));

        String text = extract(pdf);

        assertTrue(text.contains("Informe de lectura"), text);
        assertTrue(text.contains("Resumen general"), text);
        assertTrue(text.contains("Páginas leídas en total"), text);
        assertTrue(text.contains("Calificación media"), text);
        assertTrue(text.contains("Autores más leídos"), text);
        assertTrue(text.contains("Metas del periodo"), text);
        assertTrue(text.contains("Ursula K. Le Guin"), text);
        assertTrue(text.contains("Anual 2026"), text);
        assertTrue(!text.contains("2.026"), text);
        assertTrue(text.contains("Noviembre de 2026"), text);
    }

    /**
     * Comprueba que el texto del PDF en inglés usa los textos traducidos.
     *
     * @throws IOException si el PDF generado no se puede releer
     */
    @Test
    void extraeElTextoEnIngles() throws IOException {
        byte[] pdf = generator.render(summary(), goals(), "ana_lectora",
                Locale.forLanguageTag("en"));

        String text = extract(pdf);

        assertTrue(text.contains("Reading report"), text);
        assertTrue(text.contains("Overview"), text);
        assertTrue(text.contains("Pages read in total"), text);
        assertTrue(text.contains("Most read authors"), text);
        assertTrue(text.contains("Goals for the period"), text);
        assertTrue(text.contains("Year 2026"), text);
        assertTrue(text.contains("November 2026"), text);
    }

    /**
     * Comprueba que el texto del PDF en alemán conserva las diéresis.
     *
     * @throws IOException si el PDF generado no se puede releer
     */
    @Test
    void extraeElTextoEnAlemanConDieresis() throws IOException {
        byte[] pdf = generator.render(summary(), goals(), "ana_lectora",
                Locale.forLanguageTag("de"));

        String text = extract(pdf);

        assertTrue(text.contains("Lesebericht"), text);
        assertTrue(text.contains("Überblick"), text);
        assertTrue(text.contains("Gelesene Seiten insgesamt"), text);
        assertTrue(text.contains("Meistgelesene Autoren"), text);
        assertTrue(text.contains("Ziele im Zeitraum"), text);
        assertTrue(text.contains("Jahr 2026"), text);
        assertTrue(text.contains("November 2026"), text);
    }

    private static String extract(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page));
                text.append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private static AnalyticsSummaryResponse summary() {
        return new AnalyticsSummaryResponse(
                2026,
                20, 4, 2, 14,
                10, 2,
                6500, 4200, 640,
                4.3, 9, 11,
                months(),
                List.of(
                        new RatingCountResponse(1, 0),
                        new RatingCountResponse(2, 1),
                        new RatingCountResponse(3, 2),
                        new RatingCountResponse(4, 3),
                        new RatingCountResponse(5, 3)),
                List.of(
                        new AuthorCountResponse("Ursula K. Le Guin", 4),
                        new AuthorCountResponse("Octavia Butler", 3)));
    }

    private static List<MonthlyActivityResponse> months() {
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new MonthlyActivityResponse(month, month % 3, month * 50L))
                .toList();
    }

    private static List<PdfReportGenerator.GoalView> goals() {
        return List.of(
                new PdfReportGenerator.GoalView(GoalPeriod.ANNUAL, 2026, null, 24, 10, 42),
                new PdfReportGenerator.GoalView(GoalPeriod.MONTHLY, 2026, 11, 2, 2, 100));
    }
}
