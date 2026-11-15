package fr.sorbonne_u.gutenDAAR.search;

import fr.sorbonne_u.gutenDAAR.database.services.PostingService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The vocabulary: the distinct indexed terms of all the books, kept in memory.
 *
 * <p>
 * Loaded once the database is set up, by {@link #load()}.
 * </p>
 *
 * <p>
 * The list is unmodifiable and published through a volatile field,
 * so it can be read by any number of threads without synchronization.
 * </p>
 */
@Component
public class Vocabulary {

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(Vocabulary.class);

    /** The injected posting service. */
    protected final PostingService postingService;

    /** The distinct terms, sorted, or {@code null} until {@link #load()} is called. */
    protected volatile @Nullable List<String> distinctTerms;

    public Vocabulary(PostingService postingService) {
        this.postingService = postingService;
    }

    /**
     * Load (or reload) the distinct terms from the database.
     * Must be called once the setup is completed.
     */
    public void load() {
        distinctTerms = postingService.findDistinctTerms();
        LOG.debug("Vocabulary (distinct indexed terms) loaded in memory.");
    }

    /**
     * @return                          The distinct terms, sorted (unmodifiable list).
     * @throws IllegalStateException    If the vocabulary is not loaded yet.
     */
    public List<String> getDistinctTerms() {
        List<String> terms = distinctTerms;
        if (terms == null) {
            throw new IllegalStateException("The database setup is not completed.");
        }
        LOG.debug("Vocabulary (distinct indexed terms) got.");
        return terms;
    }
}