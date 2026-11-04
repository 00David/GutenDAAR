package fr.sorbonne_u.gutenDAAR.database.services;

import fr.sorbonne_u.gutenDAAR.database.entities.BookText;
import fr.sorbonne_u.gutenDAAR.database.repositories.BookTextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service allowing to interact with {@link BookTextRepository}.
 */
@Service
public class BookTextService {
    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(BookTextService.class);

    /** The injected book text repository. */
    protected final BookTextRepository bookTextRepository;

    public BookTextService(BookTextRepository bookTextRepository) {
        this.bookTextRepository = bookTextRepository;
    }

    /**
     * Insert the given book text.
     * @param bookText     The book texts to insert in the database.
     */
    public void insert(BookText bookText) {
        bookTextRepository.save(bookText);
        LOG.debug("Inserted book {} text.", bookText.getId());
    }

    /**
     * Potentially get the given book text.
     * @param bookId     The book whose text is potentially returned.
     */
    public Optional<BookText> findById(int bookId) {
        Optional<BookText> text = bookTextRepository.findById(bookId);
        LOG.debug("Got book {} text ? {}", bookId, text.isPresent());
        return text;
    }
}
