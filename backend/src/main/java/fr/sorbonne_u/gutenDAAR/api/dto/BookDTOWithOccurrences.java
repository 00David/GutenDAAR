package fr.sorbonne_u.gutenDAAR.api.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;

import java.util.List;
import java.util.Map;

/**
 * Book of the search results as sent to the frontend: a {@link BookDTO} with the number of occurrences
 * of the searched word(s) in the book.
 *
 * <p>Records cannot be extended, so the book is composed rather than inherited. It is unwrapped in the JSON,
 * so that the fields of the book are at the same level as {@code occurrences}, as if it extended {@link BookDTO}.</p>
 *
 * @param book          The book.
 * @param occurrences   The number of occurrences of the searched word(s) in the book.
 */
public record BookDTOWithOccurrences(@JsonUnwrapped BookDTO book, int occurrences) {

    /**
     * Build the DTO of a book entity, with its occurrences.
     * @param book          The book entity.
     * @param occurrences   The number of occurrences of the searched word(s) in the book.
     * @return              The corresponding DTO.
     */
    public static BookDTOWithOccurrences from(Book book, int occurrences) {
        return new BookDTOWithOccurrences(BookDTO.from(book), occurrences);
    }

    /**
     * Build the DTOs of a list of book entities (keeping their order), with their occurrences.
     * @param books         The book entities.
     * @param occurrences   The number of occurrences of the searched word(s), by book id.
     *                      A book absent from this map has 0 occurrences.
     * @return              The corresponding DTOs (unmodifiable list).
     */
    public static List<BookDTOWithOccurrences> from(List<Book> books, Map<Integer, Integer> occurrences) {
        return books.stream()
                .map(book -> from(book, occurrences.getOrDefault(book.getId(), 0)))
                .toList();
    }
}
