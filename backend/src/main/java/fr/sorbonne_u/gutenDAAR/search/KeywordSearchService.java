package fr.sorbonne_u.gutenDAAR.search;

import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import fr.sorbonne_u.gutenDAAR.database.services.PostingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Keyword search: the books whose index table contains the keyword.
 *
 * <p>Uses the inverted index (G1 seen from the word side): one lookup of the term in the postings table
 * returns the books containing it with their occurrences. Postgres is only used for this lookup;
 * the ranking is done here, so that it can later combine other weights (tf-idf, centrality).</p>
 *
 * <p>Ranking (for now): by number of occurrences of the keyword in the book, descending.
 * Ties are broken by book id, so that the order (and thus the pagination) is stable.</p>
 */
@Service
public class KeywordSearchService {

    /** The injected posting service. */
    protected final PostingService postingService;

    public KeywordSearchService(PostingService postingService) {
        this.postingService = postingService;
    }

    /**
     * Normalize a query keyword like the indexed terms, otherwise "Whale" would never match "whale".
     *
     * <p>Must stay identical to the tokenization of the books
     * ({@code Library.treatRetrievedText}: lowercase only, for now). To be shared with it once the
     * tokenization is fixed (bug_to_fix.md, B1).</p>
     *
     * @param keyword   The keyword typed by the user.
     * @return          The normalized keyword.
     */
    public static String normalize(String keyword) {
        return keyword.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Search the books containing a keyword.
     * @param keyword   The keyword, a single word.
     * @return          The matched word and every book containing it, ranked by occurrences.
     */
    public SearchResult search(String keyword) {
        String term = normalize(keyword);
        List<Posting> postings = postingService.findByTerm(term);
        if (postings.isEmpty()) {
            return SearchResult.empty();
        }

        List<ScoredBook> ranked = new ArrayList<>(postings.size());
        for (Posting posting : postings) {
            ranked.add(new ScoredBook(posting.getBookId(), posting.getOccurrences()));
        }
        ranked.sort(Comparator.comparingDouble(ScoredBook::score).reversed()
                .thenComparingInt(ScoredBook::bookId));

        return new SearchResult(List.of(term), ranked);
    }
}
