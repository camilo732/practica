package com.librolog.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenLibraryGenreTest {

    @Test
    void ignoraTemasQueNoSonGenero() {
        assertEquals("Fiction", OpenLibraryClient.genre(
                List.of("Accessible book", "Protected DAISY", "fiction", "Magic")));
    }

    @Test
    void devuelveNuloSiNoHayTemas() {
        assertNull(OpenLibraryClient.genre(null));
        assertNull(OpenLibraryClient.genre(List.of("In library")));
    }
}
