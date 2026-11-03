package fr.sorbonne_u.gutenDAAR.api.dto;

import fr.sorbonne_u.gutenDAAR.database.entities.Book;

/**
 * Book as sent to the frontend.
 *
 * <p>Kept separate from the {@link Book} entity, so that the database layout can change
 * without changing the JSON contract of the API.</p>
 *
 * @param id            The unique Gutenberg book id.
 * @param title         The book title.
 * @param author        The book author(s), or null if unknown.
 * @param language      The book language code (e.g. "en").
 * @param wordCount     The number of words of the book.
 * @param centrality    The book centrality in the document graph, or null if not computed yet.
 * @param coverUrl      The API path of the book cover.
 * @param gutenbergUrl  The page of the book on Project Gutenberg.
 */
public record BookDTO(int id, String title, String author, String language, int wordCount,
                      Double centrality, String coverUrl, String gutenbergUrl) {

    /**
     * Build the DTO of a book entity.
     * @param book      The book entity.
     * @return          The corresponding DTO.
     */
    public static BookDTO from(Book book) {
        int id = book.getId();
        return new BookDTO(
                id,
                book.getTitle(),
                book.getAuthor(),
                book.getLanguage(),
                book.getWordCount(),
                book.getCentrality(),
                "/api/books/" + id + "/cover",
                "https://www.gutenberg.org/ebooks/" + id
        );
    }
}
