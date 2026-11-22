package com.librolog.service;

import com.librolog.dto.response.ImportReportResponse;
import com.librolog.entity.Book;
import com.librolog.entity.Review;
import com.librolog.entity.UserBook;
import com.librolog.entity.enums.BookStatus;
import com.librolog.repository.BookRepository;
import com.librolog.repository.ReviewRepository;
import com.librolog.repository.UserBookRepository;
import com.librolog.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Importación de bibliotecas desde CSV.
 *
 * <p>Cada fila se procesa en su propia transacción: si una falla, las anteriores
 * se conservan y el informe indica cuál quedó fuera. Los libros ya presentes se
 * omiten sin duplicar la entrada.</p>
 */
@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    /** Tamaño máximo del archivo: 10 MB. */
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    /** Número máximo de errores detallados en la respuesta. */
    private static final int MAX_REPORTED_ERRORS = 50;

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param bookRepository      repositorio del catálogo
     * @param userBookRepository  repositorio de la biblioteca
     * @param reviewRepository    repositorio de reseñas
     * @param userRepository      repositorio de usuarios
     * @param transactionManager  gestor de transacciones
     */
    public ImportService(BookRepository bookRepository,
                         UserBookRepository userBookRepository,
                         ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         PlatformTransactionManager transactionManager) {
        this.bookRepository = bookRepository;
        this.userBookRepository = userBookRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Importa un archivo CSV en la biblioteca de un usuario.
     *
     * @param userId usuario propietario
     * @param file   archivo subido
     * @return informe con contadores y errores
     * @throws IllegalArgumentException si el archivo está vacío, es demasiado
     *                                  grande o su formato no se reconoce
     */
    public ImportReportResponse importCsv(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo CSV.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException(
                    "El archivo supera el máximo de 10 MB.");
        }

        String content;
        try {
            content = decode(file.getBytes());
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo.", ex);
        }

        CsvImportParser.Result parsed = CsvImportParser.parse(content);

        List<ImportReportResponse.RowError> errors = new ArrayList<>();
        for (CsvImportParser.RowError error : parsed.errors()) {
            if (errors.size() < MAX_REPORTED_ERRORS) {
                errors.add(new ImportReportResponse.RowError(error.line(), error.message()));
            }
        }

        int importedBooks = 0;
        int importedReviews = 0;
        int skipped = 0;
        int rowFailures = 0;

        for (CsvImportParser.ParsedRow row : parsed.rows()) {
            try {
                Outcome outcome = transactionTemplate.execute(
                        status -> importRow(userId, row));

                if (outcome == Outcome.IMPORTED) {
                    importedBooks++;
                } else if (outcome == Outcome.IMPORTED_WITH_REVIEW) {
                    importedBooks++;
                    importedReviews++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException ex) {
                rowFailures++;
                log.warn("No se pudo importar la fila {}: {}", row.line(), ex.getMessage());
                if (errors.size() < MAX_REPORTED_ERRORS) {
                    errors.add(new ImportReportResponse.RowError(
                            row.line(), "No se pudo importar esta fila."));
                }
            }
        }

        return new ImportReportResponse(
                parsed.source().name(),
                parsed.totalRows(),
                importedBooks,
                importedReviews,
                skipped,
                parsed.errors().size() + rowFailures,
                errors);
    }

    /**
     * Importa una fila dentro de una transacción.
     *
     * @param userId usuario propietario
     * @param row    fila validada
     * @return resultado de la fila
     */
    private Outcome importRow(Long userId, CsvImportParser.ParsedRow row) {
        Book book = resolveBook(row);

        if (userBookRepository.existsByUserIdAndBookId(userId, book.getId())) {
            return Outcome.SKIPPED;
        }

        UserBook entry = new UserBook();
        entry.setUser(userRepository.getReferenceById(userId));
        entry.setBook(book);
        entry.setStatus(row.status());
        entry.setTotalPages(row.totalPages());
        entry.setCurrentPage(row.currentPage());
        entry.setStartedAt(row.startedAt());
        entry.setFinishedAt(row.status() == BookStatus.READ ? row.finishedAt() : null);
        userBookRepository.saveAndFlush(entry);

        if (row.rating() != null && row.status() == BookStatus.READ) {
            Review review = new Review();
            review.setUserBook(entry);
            review.setRating(row.rating().shortValue());
            reviewRepository.save(review);
            return Outcome.IMPORTED_WITH_REVIEW;
        }
        return Outcome.IMPORTED;
    }

    private Book resolveBook(CsvImportParser.ParsedRow row) {
        if (row.isbn() != null) {
            Book byIsbn = bookRepository.findFirstByIsbnOrderByIdAsc(row.isbn())
                    .orElse(null);
            if (byIsbn != null) {
                return byIsbn;
            }
        }

        List<Book> matches = bookRepository.findByTitleAndAuthor(
                row.title(), row.author(), PageRequest.of(0, 1));
        if (!matches.isEmpty()) {
            return matches.get(0);
        }

        Book book = new Book();
        book.setTitle(row.title());
        book.setAuthor(row.author());
        book.setIsbn(row.isbn());
        book.setPublishYear(row.publishYear());
        if (row.totalPages() > 0) {
            book.setTotalPages(row.totalPages());
        }
        return bookRepository.save(book);
    }

    private static String decode(byte[] bytes) {
        int offset = 0;
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            return decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset))
                    .toString();
        } catch (CharacterCodingException ex) {
            return new String(bytes, offset, bytes.length - offset,
                    StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * Resultado de importar una fila.
     */
    private enum Outcome {

        /** Libro importado sin reseña. */
        IMPORTED,

        /** Libro importado con su reseña. */
        IMPORTED_WITH_REVIEW,

        /** El libro ya estaba en la biblioteca. */
        SKIPPED
    }
}
