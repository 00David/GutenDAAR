package fr.sorbonne_u.gutenDAAR.api.controllers;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.api.dto.BookDTO;
import fr.sorbonne_u.gutenDAAR.api.dto.SearchResponseDTO;
import fr.sorbonne_u.gutenDAAR.api.dto.SearchResultDTO;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
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
import java.util.Map;
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
    protected final KeywordSearchService keywordSearch;
    /** The injected recommendation service. */
    protected final RecommendationService recommendations;
    /** The injected book service. */
    protected final BookService bookService;

    public SearchController(KeywordSearchService keywordSearch, RecommendationService recommendations,
                            BookService bookService) {
        this.keywordSearch = keywordSearch;
        this.recommendations = recommendations;
        this.bookService = bookService;
    }

    /**
     * Keyword search: the books whose index table contains the keyword.
     * @param q     The keyword, a single word.
     * @param page  The page number, from 0.
     * @param size  The page size, from 1 to {@link Settings#MAX_PAGE_SIZE}.
     * @return      The ranked books of the page, with recommendations.
     * @throws ResponseStatusException  400 if the keyword is empty or has several words, or if the paging is invalid.
     */
    @GetMapping("/keyword")
    public SearchResponseDTO keyword(@RequestParam String q,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "" + Settings.DEFAULT_PAGE_SIZE) int size) {
        long start = System.nanoTime();
        String keyword = q.strip();
        if (keyword.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The keyword is empty.");
        }
        if (keyword.chars().anyMatch(Character::isWhitespace)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a single word is supported.");
        }
        checkPaging(page, size);

        SearchResult result = keywordSearch.search(keyword);
        return buildResponse(q, SearchMode.KEYWORD, result, page, size, start);
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
     * @param result        The ranked result of the algorithm.
     * @param page          The page number.
     * @param size          The page size.
     * @param start         The {@link System#nanoTime()} when the request started.
     * @return              The response.
     */
    protected SearchResponseDTO buildResponse(String query, SearchMode searchMode, SearchResult result,
                                              int page, int size, long start) {
        List<ScoredBook> ranked = result.ranked();

        // Page of results
        int from = (int) Math.min((long) page * size, ranked.size());
        int to = Math.min(from + size, ranked.size());
        List<ScoredBook> pageBooks = ranked.subList(from, to);

        // Recommendations: neighbours of the top results, never a book of the results
        List<Integer> topIds = new ArrayList<>();
        for (int i = 0; i < ranked.size() && i < Settings.RECOMMENDATION_SOURCES; i++) {
            topIds.add(ranked.get(i).bookId());
        }
        Set<Integer> resultIds = new HashSet<>();
        for (ScoredBook sb : ranked) {
            resultIds.add(sb.bookId());
        }
        List<Integer> recommendedIds = recommendations.recommend(topIds, resultIds, Settings.MAX_RECOMMENDATIONS);

        // Load the books of the page and of the recommendations in one query
        List<Integer> toLoad = new ArrayList<>(recommendedIds);
        for (ScoredBook sb : pageBooks) {
            toLoad.add(sb.bookId());
        }
        Map<Integer, Book> books = bookService.findAllById(toLoad);

        List<SearchResultDTO> results = new ArrayList<>(pageBooks.size());
        for (ScoredBook sb : pageBooks) {
            Book book = books.get(sb.bookId());
            if (book != null) {
                results.add(SearchResultDTO.from(book, sb.score()));
            }
        }
        List<BookDTO> recommended = new ArrayList<>(recommendedIds.size());
        for (int id : recommendedIds) {
            Book book = books.get(id);
            if (book != null) {
                recommended.add(BookDTO.from(book));
            }
        }

        double timeMs = (System.nanoTime() - start) / 1_000_000.0;
        return new SearchResponseDTO(query, searchMode, timeMs, ranked.size(), page, size,
                result.matchedWords(), results, recommended);
    }
}
