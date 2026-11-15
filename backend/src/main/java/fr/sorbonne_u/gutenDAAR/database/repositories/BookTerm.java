package fr.sorbonne_u.gutenDAAR.database.repositories;

/**
 * Projection of a posting on its book and term only: no posting entity is loaded.
 *
 * @param bookId    The unique Gutenberg book id.
 * @param term      A term contained by this book.
 */
public record BookTerm(int bookId, String term) {}
