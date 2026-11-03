package fr.sorbonne_u.gutenDAAR.api.dto;

import java.util.List;

/**
 * Response of every search endpoint (keyword, regex on words, regex on full text): same shape for all.
 *
 * @param query             The query as typed by the user.
 * @param mode              The search algorithm: "keyword", "regex-words" or "regex-fulltext".
 * @param timeMs            Server-side time to answer, in milliseconds (search + ranking + recommendations).
 * @param total             The total number of matching books, over all pages.
 * @param page              The page number, from 0.
 * @param size              The page size.
 * @param matchedWords      The indexed words that matched the query.
 * @param results           The matching books of this page, the most relevant first.
 * @param recommendations   Books close to the top results in the document graph, not among the results.
 */
public record SearchResponseDTO(String query, String mode, double timeMs, int total, int page, int size,
                                List<String> matchedWords, List<SearchResultDTO> results,
                                List<BookDTO> recommendations) {}
