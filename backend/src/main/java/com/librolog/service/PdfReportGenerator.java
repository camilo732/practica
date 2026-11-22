package com.librolog.service;

import com.librolog.config.I18nConfig;
import com.librolog.dto.response.AnalyticsSummaryResponse;
import com.librolog.dto.response.AuthorCountResponse;
import com.librolog.dto.response.MonthlyActivityResponse;
import com.librolog.entity.enums.GoalPeriod;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Genera el informe de lectura en PDF.
 *
 * <p>Todos los textos salen de {@code messages.properties} en el idioma activo,
 * asÃ­ que el mismo informe se produce en espaÃ±ol o en inglÃ©s sin duplicar la
 * maquetaciÃ³n.</p>
 */
@Component
public class PdfReportGenerator {

    private static final Color PRIMARY = new Color(31, 58, 95);
    private static final Color ACCENT = new Color(180, 83, 9);
    private static final Color MUTED = new Color(71, 85, 105);
    private static final Color BORDER = new Color(203, 213, 225);
    private static final Color HEADER_BACKGROUND = new Color(226, 232, 240);

    private final MessageSource messageSource;

    /**
     * Crea el generador con la fuente de mensajes.
     *
     * @param messageSource textos localizados
     */
    public PdfReportGenerator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Meta resumida para el informe.
     *
     * @param period      periodicidad
     * @param year        aÃ±o
     * @param month       mes, solo en metas mensuales
     * @param targetBooks libros objetivo
     * @param booksRead   libros terminados
     * @param booksPercent porcentaje de avance
     */
    public record GoalView(GoalPeriod period, int year, Integer month,
                           int targetBooks, long booksRead, int booksPercent) {
    }

    /**
     * Genera el PDF del informe.
     *
     * @param summary  resumen estadÃ­stico
     * @param goals    metas del periodo
     * @param username nombre de usuario
     * @param locale   idioma activo
     * @return contenido binario del PDF
     * @throws IllegalStateException si OpenPDF no puede construir el documento
     */
    public byte[] render(AnalyticsSummaryResponse summary,
                         List<GoalView> goals,
                         String username,
                         Locale locale) {

        Locale active = locale == null ? I18nConfig.DEFAULT_LOCALE : locale;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 42, 42, 48, 54);

        try {
            PdfWriter.getInstance(document, output);
            document.addTitle(text("pdf.title", null, active));
            document.addAuthor("LibroLog");
            document.addCreator("LibroLog");
            document.open();

            addHeader(document, summary, username, active);
            addSummary(document, summary, active);
            addMonthly(document, summary, active);
            addAuthors(document, summary, active);
            addGoals(document, goals, active);
            addFooter(document, active);

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el informe PDF.", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addHeader(Document document, AnalyticsSummaryResponse summary,
                           String username, Locale locale) throws DocumentException {

        Paragraph brand = new Paragraph("LibroLog",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ACCENT));
        brand.setSpacingAfter(2f);
        document.add(brand);

        Paragraph title = new Paragraph(text("pdf.title", null, locale),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY));
        title.setSpacingAfter(2f);
        document.add(title);

        Paragraph subtitle = new Paragraph(text("pdf.subtitle", null, locale),
                FontFactory.getFont(FontFactory.HELVETICA, 11, MUTED));
        subtitle.setSpacingAfter(10f);
        document.add(subtitle);

        String generated = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
                .withLocale(locale)
                .format(LocalDate.now());

        PdfPTable meta = new PdfPTable(new float[]{1.1f, 2.4f, 1.1f, 2.4f});
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(12f);
        meta.addCell(metaCell(text("pdf.user", null, locale), true));
        meta.addCell(metaCell(username == null ? "" : username, false));
        meta.addCell(metaCell(text("pdf.generated", null, locale), true));
        meta.addCell(metaCell(generated, false));
        meta.addCell(metaCell(text("pdf.period", null, locale), true));
        meta.addCell(metaCell(text("pdf.annual", new Object[]{String.valueOf(summary.year())}, locale), false));
        meta.addCell(metaCell("", true));
        meta.addCell(metaCell("", false));
        document.add(meta);
    }

    private void addSummary(Document document, AnalyticsSummaryResponse summary,
                            Locale locale) throws DocumentException {

        document.add(sectionTitle(text("pdf.summary", null, locale)));

        PdfPTable table = new PdfPTable(new float[]{3.2f, 1.4f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);

        addRow(table, text("pdf.totalBooks", null, locale), number(summary.totalBooks()));
        addRow(table, text("pdf.wantToRead", null, locale), number(summary.wantToRead()));
        addRow(table, text("pdf.reading", null, locale), number(summary.reading()));
        addRow(table, text("pdf.read", null, locale), number(summary.read()));
        addRow(table, text("pdf.finishedInPeriod", null, locale), number(summary.finishedThisYear()));
        addRow(table, text("pdf.pagesInPeriod", null, locale), number(summary.pagesReadThisYear()));
        addRow(table, text("pdf.pagesTotal", null, locale), number(summary.pagesReadTotal()));
        addRow(table, text("pdf.averageRating", null, locale),
                summary.reviewsCount() == 0
                        ? text("pdf.none", null, locale)
                        : String.format(locale, "%.1f / 5", summary.averageRating()));
        addRow(table, text("pdf.reviews", null, locale), number(summary.reviewsCount()));
        addRow(table, text("pdf.busiestMonth", null, locale), monthName(summary.busiestMonth(), locale));
        document.add(table);
    }

    private void addMonthly(Document document, AnalyticsSummaryResponse summary,
                            Locale locale) throws DocumentException {

        document.add(sectionTitle(text("pdf.monthly", null, locale)));

        PdfPTable table = new PdfPTable(new float[]{1.6f, 1f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);
        table.addCell(headerCell(text("pdf.month", null, locale)));
        table.addCell(headerCell(text("pdf.books", null, locale)));
        table.addCell(headerCell(text("pdf.pages", null, locale)));

        for (MonthlyActivityResponse month : summary.monthlyActivity()) {
            table.addCell(bodyCell(monthName(month.month(), locale)));
            table.addCell(bodyCell(number(month.booksFinished())));
            table.addCell(bodyCell(number(month.pagesRead())));
        }
        document.add(table);
    }

    private void addAuthors(Document document, AnalyticsSummaryResponse summary,
                            Locale locale) throws DocumentException {

        document.add(sectionTitle(text("pdf.topAuthors", null, locale)));

        if (summary.topAuthors().isEmpty()) {
            document.add(new Paragraph(text("pdf.none", null, locale),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, MUTED)));
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{3.2f, 1.4f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);
        table.addCell(headerCell(text("pdf.author", null, locale)));
        table.addCell(headerCell(text("pdf.books", null, locale)));

        for (AuthorCountResponse author : summary.topAuthors()) {
            table.addCell(bodyCell(author.author()));
            table.addCell(bodyCell(number(author.books())));
        }
        document.add(table);
    }

    private void addGoals(Document document, List<GoalView> goals,
                          Locale locale) throws DocumentException {

        document.add(sectionTitle(text("pdf.goals", null, locale)));

        if (goals == null || goals.isEmpty()) {
            document.add(new Paragraph(text("pdf.noGoals", null, locale),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, MUTED)));
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{2.4f, 3.2f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(8f);
        table.addCell(headerCell(text("pdf.goal", null, locale)));
        table.addCell(headerCell(text("pdf.progress", null, locale)));

        for (GoalView goal : goals) {
            table.addCell(bodyCell(goalLabel(goal, locale)));
            table.addCell(bodyCell(text("pdf.goalProgress",
                    new Object[]{goal.booksRead(), goal.targetBooks(), goal.booksPercent()},
                    locale)));
        }
        document.add(table);
    }

    private void addFooter(Document document, Locale locale) throws DocumentException {
        Paragraph footer = new Paragraph(text("pdf.footer", null, locale),
                FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED));
        footer.setSpacingBefore(16f);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private String goalLabel(GoalView goal, Locale locale) {
        if (goal.period() == GoalPeriod.ANNUAL) {
            return text("pdf.annual", new Object[]{String.valueOf(goal.year())}, locale);
        }
        return text("pdf.monthlyPeriod",
                new Object[]{monthName(goal.month() == null ? 1 : goal.month(), locale),
                        String.valueOf(goal.year())},
                locale);
    }

    private Paragraph sectionTitle(String title) {
        Paragraph paragraph = new Paragraph(title,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PRIMARY));
        paragraph.setSpacingBefore(6f);
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    private PdfPCell metaCell(String value, boolean label) {
        PdfPCell cell = new PdfPCell(new Phrase(value,
                FontFactory.getFont(label ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA,
                        9, label ? PRIMARY : MUTED)));
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell headerCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY)));
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell bodyCell(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value,
                FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED)));
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(5f);
        return cell;
    }

    private void addRow(PdfPTable table, String label, String value) {
        table.addCell(bodyCell(label));
        PdfPCell valueCell = new PdfPCell(new Phrase(value,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY)));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBorderColor(BORDER);
        valueCell.setBorderWidth(0.5f);
        valueCell.setPadding(5f);
        table.addCell(valueCell);
    }

    private String monthName(int month, Locale locale) {
        if (month < 1 || month > 12) {
            return text("pdf.none", null, locale);
        }
        String name = java.time.Month.of(month)
                .getDisplayName(TextStyle.FULL, locale);
        return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
    }

    private String number(long value) {
        return Long.toString(value);
    }

    /**
     * Nombre sugerido del archivo PDF.
     *
     * @param year   aÃ±o del informe
     * @param locale idioma activo
     * @return nombre sin rutas y con extensiÃ³n {@code .pdf}
     */
    public String filename(int year, Locale locale) {
        Locale active = locale == null ? I18nConfig.DEFAULT_LOCALE : locale;
        return messageSource.getMessage("pdf.filename", null, active) + "-" + year + ".pdf";
    }

    private String text(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, locale);
    }
}

