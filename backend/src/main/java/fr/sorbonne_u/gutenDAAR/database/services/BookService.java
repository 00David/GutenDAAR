package fr.sorbonne_u.gutenDAAR.database.services;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.repositories.BookRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service allowing to interact with {@link BookRepository}.
 */
@Service
public class BookService {
    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(BookService.class);

    /** The injected book repository. */
    protected final BookRepository bookRepository;
    /** The injected entity manager. */
    protected final EntityManager em;

    public BookService(BookRepository bookRepository, EntityManager em) {
        this.bookRepository = bookRepository;
        this.em = em;
    }

    /**
     * Insert the given books in a single transaction: if one insertion fails, none is kept.
     *
     * <p>Books must not have computed their centrality ({@code null}).</p>
     *
     * <p>
     * The books are sent to the database in JDBC batches of {@link Settings#BATCH_SIZE} rows,
     * and the persistence context is cleared after each batch, so that it does not keep
     * every inserted book in memory.
     * </p>
     *
     * @param books     The books to insert, none of which may already exist in the database.
     */
    @Transactional
    public void insert(List<Book> books) {
        for (int i = 0; i < books.size(); i++) {
            bookRepository.save(books.get(i));
            if ((i + 1) % Settings.BATCH_SIZE == 0) {
                em.flush();
                em.clear();
                LOG.debug("Saved books and cleared persistence context after {} iterations.", i);
            }
        }
        LOG.info("Inserted {} books.", books.size());
    }

    /**
     * Update an already inserted book having the given id.
     * @param id            The book unique Gutenberg id.
     * @param centrality    The computed book centrality.
     */
    @Transactional
    public void updateCentrality(int id, double centrality) {
        Book b = bookRepository.getReferenceById(id);
        b.setCentrality(centrality); // implicitly updates the book in DB
        LOG.debug("Updated centrality of {} with new value: {}.", id, centrality);
    }

    /**
     * Indicate if the database setup has been completed: the centrality is computed at its last step.
     * @return      true if there are books, and all of them have a computed centrality.
     */
    public boolean isSetupDone() {
        return bookRepository.count() > 0 && !bookRepository.existsByCentralityIsNull();
    }

    /**
     * Indicate if there is no book at all.
     * @return      true if the books table is empty.
     */
    public boolean isEmpty() {
        return bookRepository.count() == 0;
    }
}
