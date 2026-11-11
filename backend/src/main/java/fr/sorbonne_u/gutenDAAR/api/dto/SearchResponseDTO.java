package fr.sorbonne_u.gutenDAAR.api.dto;

import fr.sorbonne_u.gutenDAAR.search.RankingMethod;
import fr.sorbonne_u.gutenDAAR.search.SearchMode;

import java.util.List;

/**
 * Response of every search endpoint (keyword, regex on words, regex on full text): same shape for all.
 *
 * @param query             The query as typed by the user.
 * @param searchMode        The search mode on book words.
 * @param rankingMethod     The ranking method applied to the results.
 * @param timeMs            Server-side time to answer, in milliseconds (search + ranking + recommendations).
 * @param total             The total number of matching books, over all pages.
 * @param page              The page number, from 0.
 * @param size              The page size.
 * @param results           The matching books of this page with their occurrences, the most relevant first.
 * @param recommendations   Books close to the top results in the document graph, not among the results.
 */
public record SearchResponseDTO(String query, SearchMode searchMode, RankingMethod rankingMethod, double timeMs,
                                int total, int page, int size,
                                List<BookDTOWithOccurrences> results, List<BookDTO> recommendations) {}
