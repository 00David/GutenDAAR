package fr.sorbonne_u.gutenDAAR.api.controllers;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.Utils;
import fr.sorbonne_u.gutenDAAR.api.dto.BookDTO;
import fr.sorbonne_u.gutenDAAR.api.dto.BookDTOWithOccurrences;
import fr.sorbonne_u.gutenDAAR.api.dto.SearchResponseDTO;
import fr.sorbonne_u.gutenDAAR.database.services.BookService;
import fr.sorbonne_u.gutenDAAR.search.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Search endpoints: one endpoint per search algorithm, all returning a {@link SearchResponseDTO}.
 *
 * <p>The controller only handles HTTP: it validates the parameters, calls the search algorithm
 * (package {@code search}), paginates, adds the recommendations, and measures the time.</p>
 */
@RestController
@RequestMapping(Settings.SEARCH_API_MAPPING)
public class SearchController {

    /** The injected keyword search service. */
    protected final SearchService keywordSearch;
    /** The injected recommendation service. */
    protected final RecommendationService recommendations;
    /** The injected book service. */
    protected final BookService bookService;

    public SearchController(SearchService keywordSearch, RecommendationService recommendations,
                            BookService bookService) {
        this.keywordSearch = keywordSearch;
        this.recommendations = recommendations;
        this.bookService = bookService;
    }

    /**
     * Keyword search: the books whose index table contains the keyword.
     * @param query                     The keyword, a single word.
     * @param ranking                   The ranking method to apply, as a string.
     * @param page                      The page number, from 0.
     * @param size                      The page size, from 1 to {@link Settings#MAX_PAGE_SIZE}.
     * @return                          The ranked books of the page, with recommendations.
     * @throws ResponseStatusException  400 if the keyword is empty or has several words,
     *                                  the ranking method does not exist, or if the paging is invalid.
     */
    @GetMapping("/keyword")
    public SearchResponseDTO keyword(@RequestParam String query,
                                     @RequestParam String ranking,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "" + Settings.DEFAULT_PAGE_SIZE) int size) {
        long start = System.nanoTime();
        String keyword = query.strip();
        if (keyword.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The keyword is empty.");
        }
        if (Utils.separateWords(keyword).length > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a single word is supported.");
        }
        RankingMethod rankingMethod = getRankingEnumeration(ranking);
        checkPaging(page, size);

        List<BookDTOWithOccurrences> sortedResults = keywordSearch.searchByKeyword(keyword, rankingMethod);
        return buildResponse(query, SearchMode.keyword, rankingMethod, sortedResults, page, size, start);
    }

    /**
     * Convert the ranking method (as a string) to its enumeration.
     * @param methodString                  A ranking method.
     * @return                              Its equivalent as aan enumeration.
     * @throws ResponseStatusException      400 if not existing ranking enumeration matches the given string.
     */
    protected static RankingMethod getRankingEnumeration(String methodString) {
        try {
            return RankingMethod.from(methodString);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Check the paging parameters.
     * @param page  The page number, must be at least 0.
     * @param size  The page size, must be between 1 and {@link Settings#MAX_PAGE_SIZE}.
     * @throws ResponseStatusException  400 if a parameter is out of range.
     */
    protected static void checkPaging(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0.");
        }
        if (size < 1 || size > Settings.MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "size must be between 1 and " + Settings.MAX_PAGE_SIZE + ".");
        }
    }

    /**
     * Build the response of a search, common to all the search algorithms:
     * cut the requested page, add the recommendations, load the books, and stop the timer.
     * @param query         The query as typed by the user.
     * @param searchMode    The search mode on book words.
     * @param rankingMethod The ranking method applied to the results.
     * @param sortedResults The ranked result of the algorithm.
     * @param page          The page number.
     * @param size          The page size.
     * @param start         The {@link System#nanoTime()} when the request started.
     * @return              The response.
     */
    protected SearchResponseDTO buildResponse(String query, SearchMode searchMode, RankingMethod rankingMethod,
                                              List<BookDTOWithOccurrences> sortedResults,
                                              int page, int size, long start) {
        // Page of results
        int from = (int) Math.min((long) page * size, sortedResults.size());
        int to = Math.min(from + size, sortedResults.size());
        List<BookDTOWithOccurrences> sortedPageResults = sortedResults.subList(from, to);

        // Recommendations: neighbours of the top results, never a book of the results
        Set<Integer> topIds = new HashSet<>();
        for (int i = 0; i < sortedPageResults.size() && i < Settings.RECOMMENDATION_SOURCES; i++) {
            topIds.add(sortedPageResults.get(i).book().id());
        }
        Set<Integer> resultIds = new HashSet<>();
        for (BookDTOWithOccurrences result : sortedPageResults) {
            resultIds.add(result.book().id());
        }
        List<BookDTO> recommended = recommendations.recommend(topIds, resultIds, Settings.MAX_RECOMMENDATIONS);

        double timeMs = (System.nanoTime() - start) / 1_000_000.0;
        return new SearchResponseDTO(query, searchMode, rankingMethod, timeMs, sortedResults.size(), page, size,
                sortedPageResults, recommended);
    }
}
