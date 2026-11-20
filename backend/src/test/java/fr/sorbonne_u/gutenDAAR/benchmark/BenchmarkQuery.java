package fr.sorbonne_u.gutenDAAR.benchmark;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.search.RankingMethod;
import fr.sorbonne_u.gutenDAAR.search.SearchMode;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A search request whose response time is measured by {@link SearchBenchmark}.
 *
 * <p>Only the parameters changing the response time vary: the page is always 0, and the only centrality
 * ranking used is PageRank (the other centralities are ranked the same way, thus times would be the same).</p>
 *
 * @param id            The request number, for instance {@code R1}.
 * @param mode          The search mode.
 * @param query         The keyword or the regex.
 * @param ranking       The ranking method.
 * @param repetitions   The number of measured runs, averaged.
 */
public record BenchmarkQuery(String id, SearchMode mode, String query, RankingMethod ranking, int repetitions) {

    /** Number of measured runs of a keyword or regex (on indexed terms) search. */
    public static final int FAST_REPETITIONS = 20;
    /** Number of measured runs of a regex search on the full text. */
    public static final int FULL_TEXT_REPETITIONS = 10;
    /** Page requested, always the first one. */
    public static final int PAGE = 0;

    /** The measured requests, in order. */
    public static final List<BenchmarkQuery> QUERIES = List.of(
            // Keyword
            keyword("R1", "whale", RankingMethod.occurrences),      // few books
            keyword("R2", "whale", RankingMethod.pagerank),
            keyword("R3", "the", RankingMethod.occurrences),        // almost every English book
            keyword("R4", "the", RankingMethod.pagerank),
            keyword("R5", "zzzzqx", RankingMethod.occurrences),     // no book: baseline
            keyword("R6", "zzzzqx", RankingMethod.pagerank),

            // Regex on indexed terms
            regex("R7", "whale"),                                   // a single term
            regex("R8", "wh(a|e)l.*"),                              // a few terms
            regex("R9", ".*tion"),                                  // many terms
            regex("R10", "[a-z]+"),                                 // almost the whole vocabulary

            // Regex on the full text
            fullText("R11", "the"),                                 // matches early in most books (header)
            fullText("R12", "Mr\\. [A-Z][a-z]+"),                   // frequent, but only in the text itself
            fullText("R13", "white.whale"),                         // rare: most books fully scanned
            fullText("R14", "(?i)once upon a time"),                // rare and case-insensitive (vs R13)
            fullText("R15", "zzzzqx")                               // never matches: every book fully scanned
    );

    private static BenchmarkQuery keyword(String id, String query, RankingMethod ranking) {
        return new BenchmarkQuery(id, SearchMode.keyword, query, ranking, FAST_REPETITIONS);
    }

    private static BenchmarkQuery regex(String id, String query) {
        return new BenchmarkQuery(id, SearchMode.regex, query, RankingMethod.pagerank, FAST_REPETITIONS);
    }

    private static BenchmarkQuery fullText(String id, String query) {
        return new BenchmarkQuery(id, SearchMode.regex_fulltext, query, RankingMethod.pagerank, FULL_TEXT_REPETITIONS);
    }

    /**
     * Build the URL of this request.
     * @param baseUrl   The server URL, without trailing slash, for instance {@code http://localhost}.
     * @return          The full URL, with the encoded parameters.
     */
    public URI uri(String baseUrl) {
        String endpoint = switch (mode) {
            case keyword -> "/keyword";
            case regex -> "/regex/words";
            case regex_fulltext -> "/regex/fulltext";
        };
        return URI.create(baseUrl + Settings.SEARCH_API_MAPPING + endpoint
                + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&ranking=" + ranking
                + "&page=" + PAGE);
    }
}
