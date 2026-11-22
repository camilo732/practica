package com.librolog.controller;

import com.librolog.dto.response.AnalyticsMonthlyResponse;
import com.librolog.dto.response.AnalyticsSummaryResponse;
import com.librolog.entity.enums.GoalPeriod;
import com.librolog.security.AuthenticatedUser;
import com.librolog.service.AnalyticsService;
import com.librolog.service.PdfReportGenerator;
import com.librolog.service.ReadingGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * Endpoints de estadísticas y del informe PDF.
 *
 * <p>Los datos se calculan en el momento de la consulta y pertenecen siempre al
 * usuario del token. El informe se genera en el idioma activo, negociado con
 * {@code Accept-Language} o con el parámetro {@code lang}.</p>
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Estadísticas", description = "Resumen de lectura, actividad mensual e informe PDF")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ReadingGoalService goalService;
    private final PdfReportGenerator pdfReportGenerator;

    /**
     * Crea el controlador con sus dependencias.
     *
     * @param analyticsService    servicio de estadísticas
     * @param goalService         servicio de metas
     * @param pdfReportGenerator  generador del informe
     */
    public AnalyticsController(AnalyticsService analyticsService,
                               ReadingGoalService goalService,
                               PdfReportGenerator pdfReportGenerator) {
        this.analyticsService = analyticsService;
        this.goalService = goalService;
        this.pdfReportGenerator = pdfReportGenerator;
    }

    /**
     * Devuelve el resumen estadístico.
     *
     * @param principal usuario autenticado
     * @param year      año a consultar, opcional
     * @return resumen con totales, actividad mensual y distribuciones
     */
    @GetMapping("/summary")
    @Operation(summary = "Resumen de lectura",
            description = "Devuelve los totales de la biblioteca, los libros y páginas "
                    + "del año, la calificación media y la actividad de los doce meses.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen devuelto"),
            @ApiResponse(responseCode = "400", description = "Año fuera de rango"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public AnalyticsSummaryResponse summary(
            @AuthenticationPrincipal AuthenticatedUser principal,

            @Parameter(description = "Año (2000-2100)", example = "2026")
            @RequestParam(name = "year", required = false) Integer year) {
        return analyticsService.summary(principal.id(), year);
    }

    /**
     * Devuelve la serie mensual.
     *
     * @param principal usuario autenticado
     * @param year      año a consultar, opcional
     * @return actividad de enero a diciembre
     */
    @GetMapping("/monthly")
    @Operation(summary = "Actividad por mes",
            description = "Devuelve los libros terminados y las páginas leídas en cada "
                    + "mes del año, siempre con doce elementos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serie devuelta"),
            @ApiResponse(responseCode = "400", description = "Año fuera de rango"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public AnalyticsMonthlyResponse monthly(
            @AuthenticationPrincipal AuthenticatedUser principal,

            @Parameter(description = "Año (2000-2100)", example = "2026")
            @RequestParam(name = "year", required = false) Integer year) {
        return analyticsService.monthly(principal.id(), year);
    }

    /**
     * Descarga el informe de lectura en PDF.
     *
     * @param principal usuario autenticado
     * @param year      año a consultar, opcional
     * @param lang      idioma {@code es}, {@code en} o {@code de}, opcional
     * @param locale    idioma negociado con {@code Accept-Language}
     * @return PDF como adjunto
     */
    @GetMapping(value = "/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Informe de lectura en PDF",
            description = "Genera un informe con el resumen, la actividad mensual, las "
                    + "autorías más leídas y las metas del año, en el idioma activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generado"),
            @ApiResponse(responseCode = "400", description = "Año o idioma no válidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido")
    })
    public ResponseEntity<byte[]> report(
            @AuthenticationPrincipal AuthenticatedUser principal,

            @Parameter(description = "Año (2000-2100)", example = "2026")
            @RequestParam(name = "year", required = false) Integer year,

            @Parameter(description = "Idioma: es, en o de", example = "es")
            @RequestParam(name = "lang", required = false) String lang,

            Locale locale) {

        Locale active = resolveLocale(lang, locale);
        AnalyticsSummaryResponse summary = analyticsService.summary(principal.id(), year);

        List<PdfReportGenerator.GoalView> goals = goalService
                .list(principal.id(), summary.year())
                .stream()
                .map(goal -> new PdfReportGenerator.GoalView(
                        goal.period(),
                        goal.year(),
                        goal.month(),
                        goal.targetBooks(),
                        goal.booksRead(),
                        goal.booksPercent()))
                .toList();

        byte[] pdf = pdfReportGenerator.render(
                summary, goals, principal.username(), active);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(pdfReportGenerator.filename(summary.year(), active))
                                .build()
                                .toString())
                .body(pdf);
    }

    private static Locale resolveLocale(String lang, Locale fallback) {
        if (lang == null || lang.isBlank()) {
            return fallback == null ? Locale.forLanguageTag("es") : fallback;
        }
        return switch (lang.trim().toLowerCase(Locale.ROOT)) {
            case "es" -> Locale.forLanguageTag("es");
            case "en" -> Locale.forLanguageTag("en");
            case "de" -> Locale.forLanguageTag("de");
            default -> throw new IllegalArgumentException(
                    "El idioma debe ser 'es', 'en' o 'de'.");
        };
    }
}
