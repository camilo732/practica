package com.librolog.service;

import com.librolog.dto.request.AddUserBookRequest;
import com.librolog.dto.request.UpdateProgressRequest;
import com.librolog.dto.request.UpdateUserBookRequest;
import com.librolog.dto.response.LibraryStatsResponse;
import com.librolog.dto.response.PageResponse;
import com.librolog.dto.response.UserBookResponse;
import com.librolog.entity.Book;
import com.librolog.entity.UserBook;
import com.librolog.entity.enums.BookStatus;
import com.librolog.exception.ConflictException;
import com.librolog.repository.UserBookRepository;
import com.librolog.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Gestión de la biblioteca personal y del progreso de lectura.
 *
 * <p>Todas las operaciones reciben el identificador del usuario autenticado y
 * filtran por él: nunca se busca una entrada solo por su identificador. Un libro
 * de otra persona produce 404, no 403, para no revelar que existe.</p>
 *
 * <p>El estado y las fechas de inicio y fin los calculan los triggers de
 * PostgreSQL. Después de cada escritura se refresca la entidad para devolver los
 * valores reales y no los que había en memoria antes del trigger.</p>
 */
@Service
public class LibraryService {

    /** Tamaño máximo de página para no permitir consultas desmedidas. */
    private static final int MAX_PAGE_SIZE = 100;

    private final UserBookRepository userBookRepository;
    private final UserRepository userRepository;
    private final BookCatalogService bookCatalogService;
    private final EntityManager entityManager;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userBookRepository repositorio de la biblioteca
     * @param userRepository     repositorio de usuarios
     * @param bookCatalogService catálogo de libros
     * @param entityManagerFactory fábrica de persistencia para refrescar entidades
     */
    public LibraryService(UserBookRepository userBookRepository,
                          UserRepository userRepository,
                          BookCatalogService bookCatalogService,
                          EntityManagerFactory entityManagerFactory) {
        this.userBookRepository = userBookRepository;
        this.userRepository = userRepository;
        this.bookCatalogService = bookCatalogService;
        this.entityManager = SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

    /**
     * Añade un libro a la biblioteca personal.
     *
     * <p>Si el alta llega solo con la clave de Open Library y la obra no está en
     * caché, se consulta el detalle remoto dentro de esta transacción. Es un
     * camino poco frecuente y con tiempo de espera acotado; el alta por
     * {@code bookId}, que es la habitual tras una búsqueda, no hace red.</p>
     *
     * @param userId  usuario propietario
     * @param request datos del alta
     * @return entrada creada con el estado calculado por la base de datos
     * @throws ConflictException        si el libro ya está en la biblioteca
     * @throws IllegalArgumentException si el progreso o las páginas no son coherentes
     */
    @Transactional
    public UserBookResponse add(Long userId, AddUserBookRequest request) {
        Book book = bookCatalogService.resolveBook(request);

        if (userBookRepository.existsByUserIdAndBookId(userId, book.getId())) {
            throw new ConflictException("Ese libro ya está en tu biblioteca.");
        }

        int totalPages = resolveTotalPages(request, book);
        int currentPage = request.currentPage() == null ? 0 : request.currentPage();
        BookStatus status = request.status() == null ? BookStatus.WANT_TO_READ : request.status();

        currentPage = normalizeProgress(status, currentPage, totalPages);

        UserBook entry = new UserBook();
        entry.setUser(userRepository.getReferenceById(userId));
        entry.setBook(book);
        entry.setStatus(status);
        entry.setTotalPages(totalPages);
        entry.setCurrentPage(currentPage);

        userBookRepository.saveAndFlush(entry);
        entityManager.refresh(entry);
        return UserBookResponse.from(entry);
    }

    /**
     * Lista la biblioteca personal de forma paginada.
     *
     * @param userId usuario propietario
     * @param status filtro opcional por estado
     * @param page   número de página, empezando en 0
     * @param size   tamaño de página
     * @return página de entradas
     */
    @Transactional(readOnly = true)
    public PageResponse<UserBookResponse> list(Long userId, BookStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(MAX_PAGE_SIZE, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<UserBook> result = status == null
                ? userBookRepository.findByUserId(userId, pageable)
                : userBookRepository.findByUserIdAndStatus(userId, status, pageable);

        return PageResponse.from(result.map(UserBookResponse::from));
    }

    /**
     * Devuelve una entrada concreta de la biblioteca.
     *
     * @param userId usuario propietario
     * @param id     identificador de la entrada
     * @return entrada encontrada
     * @throws NoSuchElementException si no existe o es de otra persona
     */
    @Transactional(readOnly = true)
    public UserBookResponse get(Long userId, Long id) {
        return UserBookResponse.from(findOwned(userId, id));
    }

    /**
     * Modifica el estado, el total de páginas o la fecha de inicio.
     *
     * @param userId  usuario propietario
     * @param id      identificador de la entrada
     * @param request cambios solicitados
     * @return entrada actualizada
     * @throws NoSuchElementException   si la entrada no pertenece al usuario
     * @throws IllegalArgumentException si las páginas no son coherentes
     */
    @Transactional
    public UserBookResponse update(Long userId, Long id, UpdateUserBookRequest request) {
        UserBook entry = findOwned(userId, id);

        if (request.totalPages() != null) {
            if (request.totalPages() < entry.getCurrentPage()) {
                throw new IllegalArgumentException(
                        "El total de páginas no puede ser menor que la página actual ("
                                + entry.getCurrentPage() + ").");
            }
            entry.setTotalPages(request.totalPages());
        }

        if (request.startedAt() != null) {
            entry.setStartedAt(request.startedAt());
        }

        if (request.status() != null) {
            applyStatus(entry, request.status());
        }

        userBookRepository.saveAndFlush(entry);
        entityManager.refresh(entry);
        return UserBookResponse.from(entry);
    }

    /**
     * Registra el avance de lectura.
     *
     * <p>El trigger de PostgreSQL crea la entrada en {@code reading_logs},
     * cambia el estado a {@code READING} y, al llegar a la última página, a
     * {@code READ} con la fecha de fin.</p>
     *
     * @param userId  usuario propietario
     * @param id      identificador de la entrada
     * @param request página actual
     * @return entrada actualizada
     * @throws NoSuchElementException   si la entrada no pertenece al usuario
     * @throws IllegalArgumentException si la página no es válida
     */
    @Transactional
    public UserBookResponse updateProgress(Long userId, Long id, UpdateProgressRequest request) {
        UserBook entry = findOwned(userId, id);
        int page = request.currentPage();

        if (entry.getTotalPages() <= 0) {
            throw new IllegalArgumentException(
                    "Indica el total de páginas del libro antes de registrar progreso.");
        }
        if (page > entry.getTotalPages()) {
            throw new IllegalArgumentException(
                    "La página actual no puede superar el total de páginas ("
                            + entry.getTotalPages() + ").");
        }

        if (page == entry.getCurrentPage()) {
            return UserBookResponse.from(entry);
        }

        entry.setCurrentPage(page);
        userBookRepository.saveAndFlush(entry);
        entityManager.refresh(entry);
        return UserBookResponse.from(entry);
    }

    /**
     * Elimina un libro de la biblioteca personal.
     *
     * @param userId usuario propietario
     * @param id     identificador de la entrada
     * @throws NoSuchElementException si no existe o es de otra persona
     */
    @Transactional
    public void delete(Long userId, Long id) {
        UserBook entry = findOwned(userId, id);
        userBookRepository.delete(entry);
    }

    /**
     * Devuelve el resumen de la biblioteca por estado.
     *
     * @param userId usuario propietario
     * @return conteos por estado
     */
    @Transactional(readOnly = true)
    public LibraryStatsResponse stats(Long userId) {
        return new LibraryStatsResponse(
                userBookRepository.countByUserId(userId),
                userBookRepository.countByUserIdAndStatus(userId, BookStatus.WANT_TO_READ),
                userBookRepository.countByUserIdAndStatus(userId, BookStatus.READING),
                userBookRepository.countByUserIdAndStatus(userId, BookStatus.READ));
    }

    private UserBook findOwned(Long userId, Long id) {
        return userBookRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe ese libro en tu biblioteca."));
    }

    private int resolveTotalPages(AddUserBookRequest request, Book book) {
        if (request.totalPages() != null) {
            return request.totalPages();
        }
        if (book.getTotalPages() != null && book.getTotalPages() > 0) {
            return book.getTotalPages();
        }
        return 0;
    }

    private int normalizeProgress(BookStatus status, int currentPage, int totalPages) {
        if (currentPage < 0) {
            throw new IllegalArgumentException("La página actual no puede ser negativa.");
        }
        if (totalPages > 0 && currentPage > totalPages) {
            throw new IllegalArgumentException(
                    "La página actual no puede superar el total de páginas (" + totalPages + ").");
        }

        return switch (status) {
            case READ -> totalPages > 0 ? totalPages : currentPage;
            case WANT_TO_READ -> 0;
            case READING -> currentPage;
        };
    }

    /**
     * Ajusta el progreso para que el trigger no contradiga el estado pedido.
     *
     * <p>El trigger pasa a {@code READING} cualquier libro pendiente con
     * progreso y a {@code READ} el que llega a la última página. Por eso marcar
     * como pendiente exige volver a la página 0, y volver a {@code READING} un
     * libro terminado retrocede una página.</p>
     */
    private void applyStatus(UserBook entry, BookStatus status) {
        entry.setStatus(status);

        switch (status) {
            case WANT_TO_READ -> entry.setCurrentPage(0);
            case READ -> {
                if (entry.getTotalPages() > 0) {
                    entry.setCurrentPage(entry.getTotalPages());
                }
            }
            case READING -> {
                if (entry.getTotalPages() > 0
                        && entry.getCurrentPage() >= entry.getTotalPages()) {
                    entry.setCurrentPage(entry.getTotalPages() - 1);
                }
            }
        }
    }
}
