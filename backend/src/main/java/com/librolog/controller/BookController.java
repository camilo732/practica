package com.librolog.controller;

import com.librolog.dto.response.BookResponse;
import com.librolog.dto.response.BookSearchResponse;
import com.librolog.service.BookCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del catálogo de libros.
 *
 * <p>La búsqueda consulta Open Library y guarda el resultado en la caché local.
 * Requiere autenticación para que nadie use la API como proxy libre.</p>
 */
@RestController
@RequestMapping("/api/books")
@Tag(name = "Catálogo", description = "Búsqueda de libros en Open Library con caché local")
public class BookController {

    private final BookCatalogService bookCatalogService;

    /**
     * Crea el controlador con sus dependencias.
     *
     * @param bookCatalogService servicio del catálogo
     */
    public BookController(BookCatalogService bookCatalogService) {
        this.bookCatalogService = bookCatalogService;
    }

    /**
     * Busca libros por título, autoría o ISBN.
     *
     * @param query texto de búsqueda, mínimo dos caracteres
     * @param limit número máximo de resultados, entre 1 y 40
     * @return resultados y si proceden de la caché
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar libros",
            description = "Busca en Open Library y guarda los resultados en la caché local. "
                    + "Si Open Library no responde, devuelve lo que ya había guardado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados devueltos"),
            @ApiResponse(responseCode = "400", description = "Consulta demasiado corta"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "503", description = "Sin conexión con Open Library y sin caché")
    })
    public BookSearchResponse search(
            @Parameter(description = "Título, autoría o ISBN", example = "cien años de soledad")
            @RequestParam("q") String query,

            @Parameter(description = "Máximo de resultados (1-40)", example = "20")
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return bookCatalogService.search(query, limit);
    }

    /**
     * Devuelve un libro de la caché local.
     *
     * @param id identificador del libro
     * @return datos del libro
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un libro",
            description = "Devuelve un libro de la caché local por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro devuelto"),
            @ApiResponse(responseCode = "401", description = "Token ausente o no válido"),
            @ApiResponse(responseCode = "404", description = "El libro no existe")
    })
    public BookResponse get(@PathVariable Long id) {
        return bookCatalogService.get(id);
    }
}
