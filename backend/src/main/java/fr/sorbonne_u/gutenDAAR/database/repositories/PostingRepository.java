package fr.sorbonne_u.gutenDAAR.database.repositories;

import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * Repository corresponding to the {@code postings} table.
 */
public interface PostingRepository extends JpaRepository<Posting, Posting.PostingId> {

    /**
     * Find the postings of a term, i.e. the books containing it with their occurrences.
     * Uses the inverted index {@code idx_postings_term}.
     * @param term      The term to look up.
     * @return          The postings of the term, in no particular order.
     */
    List<Posting> findByTerm(String term);

    /**
     * Find the words of a book, using the primary key {@code (book_id, term)} as forward index.
     *
     * <p>Only the words are selected: no {@link Posting} entity is loaded into the persistence context.</p>
     *
     * @param bookId    The unique Gutenberg book id.
     * @return          The words of this book.
     */
    @Query("SELECT p.term FROM Posting p WHERE p.bookId = :bookId")
    Set<String> findTermsByBookId(@Param("bookId") int bookId);

    /**
     * Find the vocabulary: the distinct terms of all the books.
     * @return          The distinct terms, in no particular order.
     */
    @Query("SELECT DISTINCT p.term FROM Posting p")
    List<String> findDistinctTerms();

    /**
     * Find the books containing at least one of the given terms, using the inverted index {@code idx_postings_term}.
     *
     * <p>The terms are passed as a single SQL array parameter, so their number is not limited
     * by the maximum number of bind parameters of a query.</p>
     *
     * @param terms     The terms to look up.
     * @return          The ids of the books containing at least one of the terms, in no particular order.
     */
    @Query(value = "SELECT DISTINCT book_id FROM postings WHERE term = ANY(CAST(:terms AS text[]))", nativeQuery = true)
    List<Integer> findBookIdsByTermIn(@Param("terms") String[] terms);

    /** Create the inverted index (term → book), if it does not exist yet. Requires a transaction. */
    @Modifying
    @Query(value = "CREATE INDEX IF NOT EXISTS idx_postings_term ON postings(term)", nativeQuery = true)
    void createInvertedIndex();

    /** Refresh the planner statistics of the {@code postings} table. Requires a transaction. */
    @Modifying
    @Query(value = "ANALYZE postings", nativeQuery = true)
    void analyze();
}
