package fr.sorbonne_u.gutenDAAR.api.dto;

import fr.sorbonne_u.gutenDAAR.database.entities.Book;

/**
 * One book of the search results, as sent to the frontend.
 *
 * @param id                        The unique Gutenberg book id.
 * @param title                     The book title.
 * @param author                    The book author(s), or null if unknown.
 * @param language                  The book language code (e.g. "en").
 * @param closenessCentrality       The book closeness centrality in the document graph, or null if not computed yet.
 * @param betweennessCentrality     The book betweenness centrality in the document graph, or null if not computed yet.
 * @param pagerankCentrality        The book pagerank centrality in the document graph, or null if not computed yet.
 * @param score                     The relevance score of the book for this search.
 */
public record SearchResultDTO(int id, String title, String author, String language,
                              Double closenessCentrality, Double betweennessCentrality, Double pagerankCentrality,
                              double score) {

    /**
     * Build the DTO of a book found by a search.
     * @param book      The book entity.
     * @param score     Its relevance score for the search.
     * @return          The corresponding DTO.
     */
    public static SearchResultDTO from(Book book, double score) {
        BookDTO b = BookDTO.from(book);
        return new SearchResultDTO(b.id(), b.title(), b.author(), b.language(),
                b.closenessCentrality(), b.betweennessCentrality(), b.pagerankCentrality(),
                score);
    }
}
