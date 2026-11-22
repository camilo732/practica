package com.librolog.service;

import com.librolog.entity.enums.BookStatus;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;

/**
 * Escribe la biblioteca personal en CSV.
 *
 * <p>El formato es el mismo que acepta la importación, de modo que un archivo
 * exportado se puede volver a importar sin cambios. Se incluye una cabecera
 * legible y fechas ISO.</p>
 */
public final class LibraryCsvWriter {

    /** Columnas del CSV, en orden. */
    public static final String[] HEADER = {
            "title", "author", "status", "currentPage", "totalPages",
            "startedAt", "finishedAt", "isbn", "publishYear", "rating"
    };

    private LibraryCsvWriter() {
    }

    /**
     * Fila de exportación.
     *
     * @param title       título
     * @param author      autoría
     * @param status      estado de lectura
     * @param currentPage página actual
     * @param totalPages  total de páginas
     * @param startedAt   fecha de inicio
     * @param finishedAt  fecha de fin
     * @param isbn        ISBN
     * @param publishYear año de publicación
     * @param rating      calificación de la reseña, si existe
     */
    public record Row(String title, String author, BookStatus status,
                      int currentPage, int totalPages,
                      LocalDate startedAt, LocalDate finishedAt,
                      String isbn, Integer publishYear, Integer rating) {
    }

    /**
     * Serializa las filas a CSV.
     *
     * @param rows filas de la biblioteca
     * @return texto CSV con cabecera y saltos de línea de Windows
     */
    public static String write(List<Row> rows) {
        StringWriter output = new StringWriter();

        try (ICSVWriter writer = new CSVWriterBuilder(output)
                .withSeparator(',')
                .withQuoteChar('"')
                .withLineEnd("\r\n")
                .build()) {

            writer.writeNext(HEADER, false);

            for (Row row : rows) {
                writer.writeNext(new String[]{
                        value(row.title()),
                        value(row.author()),
                        row.status() == null ? "" : row.status().name(),
                        Integer.toString(row.currentPage()),
                        Integer.toString(row.totalPages()),
                        value(row.startedAt()),
                        value(row.finishedAt()),
                        value(row.isbn()),
                        row.publishYear() == null ? "" : Integer.toString(row.publishYear()),
                        row.rating() == null ? "" : Integer.toString(row.rating())
                }, false);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el CSV.", ex);
        }

        return output.toString();
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
