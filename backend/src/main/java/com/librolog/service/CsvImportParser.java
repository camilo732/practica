package com.librolog.service;

import com.librolog.entity.enums.BookStatus;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lector de archivos CSV para importar bibliotecas.
 *
 * <p>Reconoce dos formatos por su cabecera:</p>
 * <ul>
 *   <li><strong>LibroLog</strong>: el que genera la propia exportación.</li>
 *   <li><strong>Goodreads</strong>: la exportación oficial de la cuenta.</li>
 * </ul>
 *
 * <p>Es una clase pura: recibe el texto ya decodificado y devuelve filas
 * validadas. Los errores de una fila no invalidan el archivo completo; se
 * acumulan para que la persona sepa qué no se importó.</p>
 */
public final class CsvImportParser {

    /** Máximo de filas de datos aceptadas en un archivo. */
    public static final int MAX_ROWS = 3000;

    private static final int MAX_TEXT = 500;
    private static final int MAX_ISBN = 20;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/M/d"));

    private CsvImportParser() {
    }

    /**
     * Formato detectado.
     */
    public enum Source {

        /** Exportación de LibroLog. */
        LIBROLOG,

        /** Exportación de Goodreads. */
        GOODREADS
    }

    /**
     * Fila válida lista para importar.
     *
     * @param line        número de registro en el archivo
     * @param title       título
     * @param author      autoría, puede ser nula
     * @param status      estado de lectura
     * @param currentPage página actual
     * @param totalPages  total de páginas; 0 si se desconoce
     * @param startedAt   fecha de inicio, puede ser nula
     * @param finishedAt  fecha de fin, puede ser nula
     * @param isbn        ISBN normalizado, puede ser nulo
     * @param publishYear año de publicación, puede ser nulo
     * @param rating      calificación de 1 a 5, puede ser nula
     */
    public record ParsedRow(int line, String title, String author, BookStatus status,
                            int currentPage, int totalPages,
                            LocalDate startedAt, LocalDate finishedAt,
                            String isbn, Integer publishYear, Integer rating) {
    }

    /**
     * Fila descartada y su motivo.
     *
     * @param line    número de registro en el archivo
     * @param message motivo legible
     */
    public record RowError(int line, String message) {
    }

    /**
     * Resultado de leer un archivo.
     *
     * @param source    formato detectado
     * @param rows      filas válidas
     * @param errors    filas descartadas
     * @param totalRows filas de datos leídas, válidas o no
     */
    public record Result(Source source, List<ParsedRow> rows,
                         List<RowError> errors, int totalRows) {
    }

    /**
     * Lee y valida el contenido de un CSV.
     *
     * @param content texto completo del archivo
     * @return filas válidas, errores y formato detectado
     * @throws IllegalArgumentException si el formato no se reconoce o el archivo
     *                                  supera el máximo de filas
     */
    public static Result parse(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El archivo CSV está vacío.");
        }

        try (CSVReader reader = new CSVReaderBuilder(new StringReader(content)).build()) {
            String[] header = reader.readNext();
            if (header == null) {
                throw new IllegalArgumentException("El archivo CSV no tiene cabecera.");
            }

            Map<String, Integer> columns = headerIndex(header);
            Source source = detectSource(columns);

            List<ParsedRow> rows = new ArrayList<>();
            List<RowError> errors = new ArrayList<>();
            int totalRows = 0;
            int line = 1;
            String[] record;

            while ((record = reader.readNext()) != null) {
                line++;
                if (isBlank(record)) {
                    continue;
                }
                totalRows++;
                if (totalRows > MAX_ROWS) {
                    throw new IllegalArgumentException(
                            "El archivo supera el máximo de " + MAX_ROWS + " filas.");
                }

                ParsedRow row = source == Source.GOODREADS
                        ? parseGoodreads(record, columns, line, errors)
                        : parseLibroLog(record, columns, line, errors);

                if (row != null) {
                    rows.add(row);
                }
            }

            return new Result(source, rows, errors, totalRows);
        } catch (IOException | CsvException ex) {
            throw new IllegalArgumentException("No se pudo leer el CSV: " + ex.getMessage(), ex);
        }
    }

    private static Source detectSource(Map<String, Integer> columns) {
        if (columns.containsKey("exclusiveshelf") || columns.containsKey("myrating")) {
            require(columns, "title");
            return Source.GOODREADS;
        }
        if (columns.containsKey("currentpage") || columns.containsKey("totalpages")) {
            require(columns, "title");
            return Source.LIBROLOG;
        }
        throw new IllegalArgumentException(
                "El CSV no tiene un formato reconocido. Se aceptan exportaciones de LibroLog y de Goodreads.");
    }

    private static void require(Map<String, Integer> columns, String name) {
        if (!columns.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Al CSV le falta la columna obligatoria '" + name + "'.");
        }
    }

    private static ParsedRow parseLibroLog(String[] record,
                                           Map<String, Integer> columns,
                                           int line,
                                           List<RowError> errors) {

        String title = text(record, columns, "title", MAX_TEXT);
        if (title.isEmpty()) {
            errors.add(new RowError(line, "La fila no tiene título."));
            return null;
        }

        List<String> problems = new ArrayList<>();
        BookStatus status = parseStatus(text(record, columns, "status", 20), problems);

        int totalPages = integer(record, columns, "totalpages", 0, problems);
        int currentPage = integer(record, columns, "currentpage", 0, problems);
        LocalDate startedAt = date(record, columns, "startedat", problems);
        LocalDate finishedAt = date(record, columns, "finishedat", problems);
        Integer rating = rating(record, columns, "rating", problems);

        if (!problems.isEmpty()) {
            errors.add(new RowError(line, String.join(" ", problems)));
            return null;
        }

        int[] progress = normalize(status, currentPage, totalPages);
        if (progress == null) {
            errors.add(new RowError(line,
                    "La página actual no puede superar el total de páginas."));
            return null;
        }

        return new ParsedRow(
                line,
                title,
                blankToNull(text(record, columns, "author", MAX_TEXT)),
                status,
                progress[0],
                progress[1],
                startedAt,
                status == BookStatus.READ ? finishedAt : null,
                isbn(text(record, columns, "isbn", MAX_ISBN)),
                year(text(record, columns, "publishyear", 30)),
                rating);
    }

    private static ParsedRow parseGoodreads(String[] record,
                                            Map<String, Integer> columns,
                                            int line,
                                            List<RowError> errors) {

        String title = text(record, columns, "title", MAX_TEXT);
        if (title.isEmpty()) {
            errors.add(new RowError(line, "La fila no tiene título."));
            return null;
        }

        BookStatus status = goodreadsStatus(
                text(record, columns, "exclusiveshelf", 40));
        List<String> problems = new ArrayList<>();

        int totalPages = integer(record, columns, "numberofpages", 0, problems);
        LocalDate finishedAt = date(record, columns, "dateread", problems);
        Integer rating = rating(record, columns, "myrating", problems);

        if (!problems.isEmpty()) {
            errors.add(new RowError(line, String.join(" ", problems)));
            return null;
        }

        String isbn = isbn(firstNonBlank(
                text(record, columns, "isbn13", MAX_ISBN),
                text(record, columns, "isbn", MAX_ISBN)));

        int[] progress = normalize(status, 0, totalPages);
        if (progress == null) {
            errors.add(new RowError(line,
                    "El número de páginas no es válido."));
            return null;
        }

        return new ParsedRow(
                line,
                title,
                blankToNull(text(record, columns, "author", MAX_TEXT)),
                status,
                progress[0],
                progress[1],
                null,
                status == BookStatus.READ ? finishedAt : null,
                isbn,
                year(text(record, columns, "yearpublished", 30)),
                rating);
    }

    private static BookStatus goodreadsStatus(String shelf) {
        String value = shelf == null ? "" : shelf.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "read" -> BookStatus.READ;
            case "currently-reading" -> BookStatus.READING;
            default -> BookStatus.WANT_TO_READ;
        };
    }

    private static BookStatus parseStatus(String value, List<String> problems) {
        if (value == null || value.isBlank()) {
            return BookStatus.WANT_TO_READ;
        }
        try {
            return BookStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            problems.add("El estado '" + value + "' no es válido.");
            return BookStatus.WANT_TO_READ;
        }
    }

    /**
     * Ajusta página y total para que el trigger de la base no contradiga el
     * estado importado.
     *
     * @return página y total normalizados, o {@code null} si no son coherentes
     */
    private static int[] normalize(BookStatus status, int currentPage, int totalPages) {
        if (currentPage < 0 || totalPages < 0) {
            return null;
        }
        if (totalPages > 0 && currentPage > totalPages) {
            return null;
        }

        int page = currentPage;
        if (status == BookStatus.READ) {
            page = totalPages > 0 ? totalPages : currentPage;
        } else if (status == BookStatus.WANT_TO_READ) {
            page = 0;
        } else if (status == BookStatus.READING
                && totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }
        return new int[]{page, totalPages};
    }

    private static int integer(String[] record, Map<String, Integer> columns,
                               String name, int defaultValue, List<String> problems) {
        String value = raw(record, columns, name);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                problems.add("El campo '" + name + "' no puede ser negativo.");
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            problems.add("El campo '" + name + "' no es un número válido.");
            return defaultValue;
        }
    }

    private static LocalDate date(String[] record, Map<String, Integer> columns,
                                  String name, List<String> problems) {
        String value = raw(record, columns, name);
        if (value.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Se prueba el siguiente formato.
            }
        }

        problems.add("La fecha del campo '" + name + "' no es válida.");
        return null;
    }

    private static Integer rating(String[] record, Map<String, Integer> columns,
                                  String name, List<String> problems) {
        String value = raw(record, columns, name);
        if (value.isEmpty() || "0".equals(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 5) {
                problems.add("La calificación debe estar entre 1 y 5.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            problems.add("La calificación no es un número válido.");
            return null;
        }
    }

    private static Integer year(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (int i = 0; i + 4 <= trimmed.length(); i++) {
            String candidate = trimmed.substring(i, i + 4);
            if (candidate.chars().allMatch(Character::isDigit)) {
                int parsed = Integer.parseInt(candidate);
                if (parsed >= 1000 && parsed <= 2100) {
                    return parsed;
                }
            }
        }
        return null;
    }

    private static String isbn(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.startsWith("=\"")) {
            cleaned = cleaned.substring(2);
        }
        cleaned = cleaned.replace("\"", "")
                .replace("-", "")
                .replace(" ", "");
        if (cleaned.isEmpty() || cleaned.chars().allMatch(c -> c == '0')) {
            return null;
        }
        return cleaned.length() > MAX_ISBN ? cleaned.substring(0, MAX_ISBN) : cleaned;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String text(String[] record, Map<String, Integer> columns,
                               String name, int maxLength) {
        String value = raw(record, columns, name);
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private static String raw(String[] record, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null || index < 0 || index >= record.length) {
            return "";
        }
        String value = record[index];
        return value == null ? "" : value.trim();
    }

    private static Map<String, Integer> headerIndex(String[] header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String name = header[i] == null ? "" : header[i]
                    .replace("\uFEFF", "")
                    .trim()
                    .toLowerCase(Locale.ROOT)
                    .replace(" ", "")
                    .replace("_", "");
            if (!name.isEmpty()) {
                columns.putIfAbsent(name, i);
            }
        }
        return columns;
    }

    private static boolean isBlank(String[] record) {
        for (String value : record) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
