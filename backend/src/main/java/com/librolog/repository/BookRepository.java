package com.librolog.repository;

import com.librolog.entity.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio del catálogo y de la caché de Open Library.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Busca un libro por su clave de Open Library.
     *
     * @param openLibraryKey clave de Open Library
     * @return libro encontrado, si existe
     */
    Optional<Book> findByOpenLibraryKey(String openLibraryKey);

    /**
     * Busca un libro por ISBN.
     *
     * <p>Se usa al importar un CSV para reutilizar un libro ya cacheado en lugar
     * de crear un duplicado.</p>
     *
     * @param isbn ISBN normalizado
     * @return primer libro con ese ISBN, si existe
     */
    Optional<Book> findFirstByIsbnOrderByIdAsc(String isbn);

    /**
     * Busca un libro por título y autoría.
     *
     * <p>La autoría puede ser nula: en ese caso solo se compara el título. La
     * comparación no distingue mayúsculas ni minúsculas.</p>
     *
     * @param title  título exacto
     * @param author autoría, o {@code null}
     * @param limit  número máximo de resultados
     * @return coincidencias ordenadas por identificador
     */
    @Query("""
            select b from Book b
             where lower(b.title) = lower(:title)
               and ((:author is null and b.author is null) or lower(b.author) = lower(:author))
             order by b.id
            """)
    List<Book> findByTitleAndAuthor(@Param("title") String title,
                                    @Param("author") String author,
                                    Pageable limit);

    /**
     * Busca libros por título o autor con una consulta JPQL parametrizada.
     *
     * @param q        texto a buscar
     * @param pageable límite y orden
     * @return coincidencias
     */
    @Query("""
            select b from Book b
            where lower(b.title) like lower(concat('%', :q, '%'))
               or lower(b.author) like lower(concat('%', :q, '%'))
            order by b.title
            """)
    List<Book> search(@Param("q") String q, Pageable pageable);
}
