package fr.sorbonne_u.gutenDAAR.search;

import java.util.List;

/**
 * Result of a search algorithm, before pagination: every matching book, ranked.
 *
 * @param matchedWords  The indexed words that matched the query (one word for a keyword search,
 *                      possibly many for a regex search).
 * @param ranked        All the matching books, the most relevant first.
 */
public record SearchResult(List<String> matchedWords, List<ScoredBook> ranked) {

    /** @return An empty result, for a query matching nothing. */
    public static SearchResult empty() {
        return new SearchResult(List.of(), List.of());
    }
}
