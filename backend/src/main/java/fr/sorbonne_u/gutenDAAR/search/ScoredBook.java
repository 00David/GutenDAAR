package fr.sorbonne_u.gutenDAAR.search;

/**
 * A book matching a search, with its relevance score (the higher, the more relevant).
 *
 * @param bookId    The unique Gutenberg book id.
 * @param score     The relevance score of the book for the search.
 */
public record ScoredBook(int bookId, double score) {}
