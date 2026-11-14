package fr.sorbonne_u.gutenDAAR.api.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.search.SearchMode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Book of the search results as sent to the frontend: a {@link BookDTO} with potentially
 * the number of occurrences, in case of using {@link SearchMode#keyword}.
 *
 * <p>Records cannot be extended, so the book is composed rather than inherited. It is unwrapped in the JSON,
 * so that the fields of the book are at the same level as {@code occurrences}, as if it extended {@link BookDTO}.</p>
 *
 * @param book          The book.
 * @param occurrences   The number of occurrences of the searched word(s) in the book.
 */
public record BookSearchedDTO(@JsonUnwrapped BookDTO book, int occurrences) {

    /** Number of occurrences for each absent book of a given occurrences map. */
    public static final int ABSENT_OCCURRENCES = 0;
    /** Number of occurrences for each book when no occurrences map is provided. */
    public static final int NO_PROVIDED_OCCURRENCES = -1;

    /**
     * Build the DTO of a book entity, with its occurrences.
     * @param book          The book entity.
     * @param occurrences   The number of occurrences of the searched word(s) in the book.
     * @return              The corresponding DTO.
     */
    public static BookSearchedDTO from(Book book, int occurrences) {
        return new BookSearchedDTO(BookDTO.from(book), occurrences);
    }

    /**
     * Build the DTOs of a list of book entities (keeping their order), without providing their occurrences.
     * @param books         The book entities.
     * @return              The corresponding DTOs (unmodifiable list).
     */
    public static List<BookSearchedDTO> from(List<Book> books) {
        return from(books, null);
    }

    /**
     * Build the DTOs of a list of book entities (keeping their order), with their occurrences if given.
     * @param books         The book entities.
     * @param occurrences   The number of occurrences of the searched word(s), by book id.
     *                      A book absent from this map has {@link #ABSENT_OCCURRENCES}.
     *                      If the map is {@code null}, each book has {@link #NO_PROVIDED_OCCURRENCES}.
     * @return              The corresponding DTOs (unmodifiable list).
     */
    public static List<BookSearchedDTO> from(List<Book> books, @Nullable Map<Integer, Integer> occurrences) {
        return books.stream()
            .map(book ->
                from(book, occurrences == null
                    ? NO_PROVIDED_OCCURRENCES
                    : occurrences.getOrDefault(book.getId(), ABSENT_OCCURRENCES)
                ))
            .toList();
    }
}
