package com.librolog.client;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del mapeo de respuestas de Open Library.
 *
 * <p>Se usan mapas de ejemplo, así que las pruebas no dependen de la red ni de
 * la disponibilidad del servicio externo.</p>
 */
class OpenLibraryClientTest {

    private Map<String, Object> documentoCompleto() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("key", "/works/OL27448W");
        doc.put("title", "El Señor de los Anillos");
        doc.put("author_name", List.of("J. R. R. Tolkien", "Otro autor"));
        doc.put("first_publish_year", 1954);
        doc.put("cover_i", 1234567);
        doc.put("number_of_pages_median", 1216L);
        doc.put("isbn", List.of("9788445071779", "8445071775"));
        return doc;
    }

    /**
     * Comprueba que un documento completo se traduce a todos los campos.
     */
    @Test
    void mapeaUnDocumentoCompleto() {
        OpenLibraryBook book = OpenLibraryClient.mapDoc(documentoCompleto());

        assertNotNull(book);
        assertEquals("/works/OL27448W", book.openLibraryKey());
        assertEquals("El Señor de los Anillos", book.title());
        assertEquals("J. R. R. Tolkien", book.author());
        assertEquals("https://covers.openlibrary.org/b/id/1234567-M.jpg", book.coverUrl());
        assertEquals(1216, book.totalPages());
        assertEquals(1954, book.publishYear());
        assertEquals("9788445071779", book.isbn());
        assertNull(book.description());
    }

    /**
     * Comprueba que un documento sin título se descarta.
     */
    @Test
    void descartaDocumentosSinTitulo() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("key", "/works/OL1W");
        doc.put("author_name", List.of("Anónimo"));

        assertNull(OpenLibraryClient.mapDoc(doc));
    }

    /**
     * Comprueba que los campos ausentes quedan nulos y no rompen el mapeo.
     */
    @Test
    void toleraCamposAusentes() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("key", "/works/OL2W");
        doc.put("title", "  Libro mínimo  ");

        OpenLibraryBook book = OpenLibraryClient.mapDoc(doc);

        assertNotNull(book);
        assertEquals("Libro mínimo", book.title());
        assertNull(book.author());
        assertNull(book.coverUrl());
        assertNull(book.totalPages());
        assertNull(book.publishYear());
        assertNull(book.isbn());
    }

    /**
     * Comprueba que los números enviados como texto también se aceptan.
     */
    @Test
    void aceptaNumerosEnTexto() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("key", "/works/OL3W");
        doc.put("title", "Libro con texto numérico");
        doc.put("first_publish_year", "1999");
        doc.put("number_of_pages_median", "321");

        OpenLibraryBook book = OpenLibraryClient.mapDoc(doc);

        assertNotNull(book);
        assertEquals(1999, book.publishYear());
        assertEquals(321, book.totalPages());
    }

    /**
     * Comprueba que solo se aceptan claves de obra con el formato esperado.
     *
     * <p>Es la barrera que impide construir rutas arbitrarias con la clave.</p>
     */
    @Test
    void validaLasClavesDeObra() {
        assertTrue(OpenLibraryClient.isValidKey("/works/OL27448W"));
        assertTrue(OpenLibraryClient.isValidKey("/works/OL123ABCW"));

        assertFalse(OpenLibraryClient.isValidKey(null));
        assertFalse(OpenLibraryClient.isValidKey(""));
        assertFalse(OpenLibraryClient.isValidKey("/works/../../etc/passwd"));
        assertFalse(OpenLibraryClient.isValidKey("/authors/OL1A"));
        assertFalse(OpenLibraryClient.isValidKey("https://evil.example/works/OL1W"));
        assertFalse(OpenLibraryClient.isValidKey("/works/OL27448W?x=1"));
    }

    /**
     * Comprueba que se recorta un título desmedido al límite de la columna.
     */
    @Test
    void recortaTitulosDemasiadoLargos() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("key", "/works/OL4W");
        doc.put("title", "a".repeat(900));

        OpenLibraryBook book = OpenLibraryClient.mapDoc(doc);

        assertNotNull(book);
        assertEquals(500, book.title().length());
    }
}
