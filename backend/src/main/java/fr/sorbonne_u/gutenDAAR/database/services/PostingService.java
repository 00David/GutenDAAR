package fr.sorbonne_u.gutenDAAR.database.services;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import fr.sorbonne_u.gutenDAAR.database.repositories.BookRepository;
import fr.sorbonne_u.gutenDAAR.database.repositories.PostingRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service allowing to interact with {@link PostingRepository}.
 */
@Service
public class PostingService {
    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(PostingService.class);

    /** The injected posting repository. */
    protected final PostingRepository postingRepository;
    /** The injected book repository. */
    protected final BookRepository bookRepository;
    /** The injected entity manager. */
    protected final EntityManager em;

    public PostingService(PostingRepository postingRepository, BookRepository bookRepository, EntityManager em) {
        this.postingRepository = postingRepository;
        this.bookRepository = bookRepository;
        this.em = em;
    }

    /**
     * Insert the given postings in a single transaction: if one insertion fails, none is kept.
     *
     * <p>
     * The postings are sent to the database in JDBC batches of {@link Settings#BATCH_SIZE} rows,
     * and the persistence context is cleared after each batch, so that it does not keep
     * every inserted posting in memory.
     * </p>
     *
     * @param postings     The postings to insert, none of which may already exist in the database.
     */
    @Transactional
    public void insert(List<Posting> postings) {
        for (int i = 0; i < postings.size(); i++) {
            postingRepository.save(postings.get(i));
            if ((i + 1) % Settings.BATCH_SIZE == 0) {
                em.flush();
                em.clear();
                LOG.debug("Saved postings and cleared persistence context after {} iterations.", i);
            }
        }
        LOG.debug("Inserted {} postings.", postings.size());
    }

    /**
     * Create the inverted index (term → book) on the {@code postings} table, if it does not exist yet,
     * and refresh the planner statistics.
     *
     * <p>
     * The table's primary key {@code (book_id, term)} already is the forward index (book → term).
     * The inverted index {@code idx_postings_term} is a B-tree on the {@code term} column only: its entries
     * are sorted by term, and each one points to the table rows containing this term.
     * </p>
     * <pre>
     * idx_postings_term         postings (book_id, term, occurrences)
     * "ship"  → rows 7, 42      row 7:  (12, "ship", 3)
     * "whale" → rows 8, 90      row 8:  (12, "whale", 40)
     *                           row 42: (84, "ship", 1)
     *                           row 90: (84, "whale", 2)
     * </pre>
     *
     * <p>
     * Must be called after filling the postings table: building the index once is much
     * faster than updating it at each insertion. Later insertions update it automatically.
     * </p>
     */
    @Transactional
    public void createInvertedIndex() {
        postingRepository.createInvertedIndex();
        postingRepository.analyze();
        LOG.info("Created the inverted index on the postings table.");
    }

    /**
     * Find the postings of a book: the indexed terms contained in it (forward index lookup).
     * @param bookId    The book to look up.
     * @return          The postings of the book, in no particular order.
     */
    public List<Posting> findByBookId(Integer bookId) {
        return postingRepository.findByBookId(bookId);
    }

    /**
     * Find the postings of a term: the books containing it, with their occurrences (inverted index lookup).
     * @param term      The term to look up, already normalized like the indexed words.
     * @return          The postings of the term, in no particular order.
     */
    public List<Posting> findByTerm(String term) {
        return postingRepository.findByTerm(term);
    }

    /**
     * Get the distinct words of a book.
     * @param bookId    The unique Gutenberg book id.
     * @return          The words of this book, empty if the book is unknown or has no posting.
     */
    public Set<String> getBookTerms(int bookId) {
        Set<String> terms = postingRepository.findTermsByBookId(bookId);
        LOG.debug("Got words from book {}.", bookId);
        return terms;
    }
}
