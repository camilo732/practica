package com.librolog.service;

import com.librolog.entity.enums.BookStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del lector de CSV de importación.
 */
class CsvImportParserTest {

    private static final String GOODREADS_HEADER =
            "Title,Author,ISBN,ISBN13,My Rating,Number of Pages,"
                    + "Year Published,Date Read,Exclusive Shelf";

    /**
     * Comprueba la lectura de una fila terminada de Goodreads.
     */
    @Test
    void leeUnaFilaTerminadaDeGoodreads() {
        String csv = GOODREADS_HEADER + "\n"
                + "\"El nombre del viento, día uno\",Patrick Rothfuss,"
                + "=\"9788408267167\",=\"9788408267167\",5,880,2007,2024/01/31,read\n";

        CsvImportParser.Result result = CsvImportParser.parse(csv);

        assertEquals(CsvImportParser.Source.GOODREADS, result.source());
        assertEquals(1, result.totalRows());
        assertEquals(1, result.rows().size());
        assertTrue(result.errors().isEmpty());

        CsvImportParser.ParsedRow row = result.rows().get(0);
        assertEquals("El nombre del viento, día uno", row.title());
        assertEquals("Patrick Rothfuss", row.author());
        assertEquals(BookStatus.READ, row.status());
        assertEquals(880, row.totalPages());
        assertEquals(880, row.currentPage());
        assertEquals(LocalDate.of(2024, 1, 31), row.finishedAt());
        assertEquals("9788408267167", row.isbn());
        assertEquals(2007, row.publishYear());
        assertEquals(5, row.rating());
    }

    /**
     * Comprueba la traducción de los estantes de Goodreads.
     */
    @Test
    void traduceLosEstantesDeGoodreads() {
        String csv = GOODREADS_HEADER + "\n"
                + "Leyendo,Autora,ISBN,ISBN13,0,300,2020,,currently-reading\n"
                + "Pendiente,Autora,ISBN,ISBN13,0,200,2019,,to-read\n";

        CsvImportParser.Result result = CsvImportParser.parse(csv);

        assertEquals(2, result.rows().size());
        assertEquals(BookStatus.READING, result.rows().get(0).status());
        assertEquals(0, result.rows().get(0).currentPage());
        assertNull(result.rows().get(0).rating());
        assertEquals(BookStatus.WANT_TO_READ, result.rows().get(1).status());
        assertNull(result.rows().get(1).finishedAt());
    }

    /**
     * Comprueba que un CSV exportado por LibroLog se puede volver a importar.
     */
    @Test
    void leeElCsvExportadoPorLibroLog() {
        List<LibraryCsvWriter.Row> rows = List.of(
                new LibraryCsvWriter.Row("Cien años, de soledad", "Gabriel García Márquez",
                        BookStatus.READ, 471, 471,
                        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
                        "9788437604947", 1967, 5),
                new LibraryCsvWriter.Row("Un libro pendiente", null,
                        BookStatus.WANT_TO_READ, 0, 320,
                        null, null, null, null, null));

        String csv = LibraryCsvWriter.write(rows);
        CsvImportParser.Result result = CsvImportParser.parse(csv);

        assertEquals(CsvImportParser.Source.LIBROLOG, result.source());
        assertEquals(2, result.rows().size());
        assertTrue(result.errors().isEmpty());

        CsvImportParser.ParsedRow first = result.rows().get(0);
        assertEquals("Cien años, de soledad", first.title());
        assertEquals("Gabriel García Márquez", first.author());
        assertEquals(BookStatus.READ, first.status());
        assertEquals(471, first.currentPage());
        assertEquals(LocalDate.of(2024, 2, 1), first.finishedAt());
        assertEquals(5, first.rating());

        CsvImportParser.ParsedRow second = result.rows().get(1);
        assertEquals(BookStatus.WANT_TO_READ, second.status());
        assertNull(second.author());
        assertNull(second.startedAt());
    }

    /**
     * Comprueba que la cabecera con BOM se reconoce.
     */
    @Test
    void aceptaCabeceraConBom() {
        String csv = "\uFEFF" + GOODREADS_HEADER + "\n"
                + "Libro,Autora,ISBN,ISBN13,4,100,2000,2024/03/01,read\n";

        CsvImportParser.Result result = CsvImportParser.parse(csv);

        assertEquals(CsvImportParser.Source.GOODREADS, result.source());
        assertEquals(1, result.rows().size());
    }

    /**
     * Comprueba que las filas con errores no invalidan el archivo completo.
     */
    @Test
    void informaDeFilasInvalidasSinPerderLasValidas() {
        String csv = "title,author,status,currentPage,totalPages,startedAt,finishedAt,isbn,publishYear,rating\n"
                + "Libro válido,Autora,READ,100,100,,2024-01-01,,2000,4\n"
                + ",Autora,READ,100,100,,2024-01-01,,2000,4\n"
                + "Otro libro,Autora,READ,200,100,,2024-01-01,,2000,4\n"
                + "Tercer libro,Autora,READ,100,100,,2024-01-01,,2000,9\n";

        CsvImportParser.Result result = CsvImportParser.parse(csv);

        assertEquals(4, result.totalRows());
        assertEquals(1, result.rows().size());
        assertEquals(3, result.errors().size());
        assertEquals(3, result.errors().get(0).line());
        assertEquals(4, result.errors().get(1).line());
        assertEquals(5, result.errors().get(2).line());
    }

    /**
     * Comprueba que la página se ajusta al estado importado.
     */
    @Test
    void ajustaElProgresoSegunElEstado() {
        String csv = "title,author,status,currentPage,totalPages,startedAt,finishedAt,isbn,publishYear,rating\n"
                + "Pendiente con página,Autora,WANT_TO_READ,30,100,,,,\n"
                + "Leyendo al final,Autora,READING,100,100,,,,\n";

        CsvImportParser.Result result = CsvImportParser.parse(csv);

        assertEquals(2, result.rows().size());
        assertEquals(0, result.rows().get(0).currentPage());
        assertEquals(99, result.rows().get(1).currentPage());
        assertEquals(BookStatus.READING, result.rows().get(1).status());
    }

    /**
     * Comprueba que un formato desconocido se rechaza.
     */
    @Test
    void rechazaUnFormatoDesconocido() {
        assertThrows(IllegalArgumentException.class,
                () -> CsvImportParser.parse("nombre,edad\nAna,30\n"));
    }

    /**
     * Comprueba que un archivo vacío se rechaza.
     */
    @Test
    void rechazaUnArchivoVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> CsvImportParser.parse("   "));
    }

    /**
     * Comprueba el límite de filas.
     */
    @Test
    void rechazaArchivosDemasiadoGrandes() {
        StringBuilder csv = new StringBuilder(GOODREADS_HEADER).append('\n');
        for (int i = 0; i < CsvImportParser.MAX_ROWS + 1; i++) {
            csv.append("Libro ").append(i)
                    .append(",Autora,ISBN,ISBN13,0,100,2000,,to-read\n");
        }

        assertThrows(IllegalArgumentException.class,
                () -> CsvImportParser.parse(csv.toString()));
    }

    /**
     * Comprueba que un ISBN de ceros se descarta.
     */
    @Test
    void descartaIsbnDeCeros() {
        String csv = GOODREADS_HEADER + "\n"
                + "Libro,Autora,=\"0000000000\",=\"0000000000\",0,100,2000,,to-read\n";

        CsvImportParser.Result result = CsvImportParser.parse(csv);

        assertNull(result.rows().get(0).isbn());
    }
}
