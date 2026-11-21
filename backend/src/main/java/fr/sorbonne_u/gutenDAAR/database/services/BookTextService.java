package fr.sorbonne_u.gutenDAAR.database.services;

import fr.sorbonne_u.gutenDAAR.database.entities.BookText;
import fr.sorbonne_u.gutenDAAR.database.repositories.BookTextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Potentially get the given book text, without the Project Gutenberg header and footer.
     * @param bookId     The book whose text is potentially returned.
     * @return           The text of the book only (see {@link #stripBoilerplate(String)}).
     */
    public Optional<String> findBodyById(int bookId) {
        return findById(bookId).map(t -> stripBoilerplate(t.getContent()));
    }

    /** Line opening the book itself, e.g. "*** START OF THE PROJECT GUTENBERG EBOOK MACBETH ***". */
    protected static final Pattern START_MARKER = Pattern.compile("^[ \\t]*\\*\\*\\* ?START OF .*$",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    /** Line closing the book itself, e.g. "*** END OF THE PROJECT GUTENBERG EBOOK MACBETH ***". */
    protected static final Pattern END_MARKER = Pattern.compile("^[ \\t]*\\*\\*\\* ?END OF ",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    /**
     * Remove the Project Gutenberg header (before the "*** START OF" line) and footer (from the "*** END OF" line):
     * only the book itself is kept.
     * @param content   The full Gutenberg text.
     * @return          The text between the two marker lines, without surrounding blank lines.
     *                  A missing marker keeps the corresponding part of the text.
     */
    public static String stripBoilerplate(String content) {
        int start = 0;
        Matcher startMatcher = START_MARKER.matcher(content);
        if (startMatcher.find()) start = startMatcher.end();

        int end = content.length();
        Matcher endMatcher = END_MARKER.matcher(content);
        if (endMatcher.find(start)) end = endMatcher.start();

        return content.substring(start, end).strip();
    }
}
