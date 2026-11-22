package fr.sorbonne_u.gutenDAAR.api.controllers;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.api.dto.BookDTO;
import fr.sorbonne_u.gutenDAAR.api.dto.BookSearchedDTO;
import fr.sorbonne_u.gutenDAAR.api.dto.SearchResponseDTO;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.search.RankingMethod;
import fr.sorbonne_u.gutenDAAR.search.RecommendationService;
import fr.sorbonne_u.gutenDAAR.search.SearchMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * {@link SearchController} unit tests.
 */
class SearchControllerTest {

    /** Check that the given call is rejected with a 400 Bad Request. */
    private static void assertBadRequest(Executable call) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, call);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void validRankingMethod() {
        assertEquals(RankingMethod.pagerank, SearchController.getRankingEnumeration("PageRank"));
        assertEquals(RankingMethod.occurrences, SearchController.getRankingEnumeration("occurrences"));
    }

    @Test
    void unknownRankingMethodIsBadRequest() {
        assertBadRequest(() -> SearchController.getRankingEnumeration("popularity"));
        assertBadRequest(() -> SearchController.getRankingEnumeration(""));
    }

    @Test
    void validRegex() {
        assertTrue(SearchController.getRegex("wha.*").matcher("whale").matches());
        assertTrue(SearchController.getRegex("colou?r").matcher("color").matches());
    }

    @Test
    void invalidRegexIsBadRequest() {
        assertBadRequest(() -> SearchController.getRegex("[a-z"));  // unclosed class
        assertBadRequest(() -> SearchController.getRegex("(ab"));   // unclosed group
        assertBadRequest(() -> SearchController.getRegex("*a"));    // nothing to repeat
    }

    @Test
    void validPaging() {
        assertDoesNotThrow(() -> SearchController.checkPaging(0, 1));
        assertDoesNotThrow(() -> SearchController.checkPaging(5, Settings.MAX_PAGE_SIZE));
    }

    @Test
    void invalidPagingIsBadRequest() {
        assertBadRequest(() -> SearchController.checkPaging(-1, 20));                         // negative page
        assertBadRequest(() -> SearchController.checkPaging(0, 0));                           // empty page
        assertBadRequest(() -> SearchController.checkPaging(0, Settings.MAX_PAGE_SIZE + 1));  // page too big
    }

    // ---------- Pagination of the response (buildResponse) ----------

    /** Results of books 1 to n, in this order. */
    private static List<BookSearchedDTO> results(int n) {
        return IntStream.rangeClosed(1, n)
                .mapToObj(id -> BookSearchedDTO.from(new Book(id, "Title " + id, "Author", "en", 100), 3))
                .toList();
    }

    private static List<Integer> ids(SearchResponseDTO response) {
        return response.results().stream().map(r -> r.book().id()).toList();
    }

    /** Controller whose recommendation service is a mock (returns an empty list unless stubbed). */
    private final RecommendationService recommendations = mock(RecommendationService.class);
    private final SearchController controller = new SearchController(null, recommendations, null);

    /** Response for the given results, page and size. */
    private SearchResponseDTO response(List<BookSearchedDTO> results, int page, int size) {
        return controller.buildResponse("whale", SearchMode.keyword, RankingMethod.occurrences,
                results, page, size, System.nanoTime());
    }

    @Test
    void firstPage() {
        // 5 results, pages of 2: [1, 2] [3, 4] [5]
        SearchResponseDTO r = response(results(5), 0, 2);
        assertEquals(List.of(1, 2), ids(r));
        assertEquals(5, r.total());   // total over all the pages
        assertEquals(0, r.page());
        assertEquals(2, r.size());
        assertEquals("whale", r.query());
    }

    @Test
    void lastPageIsIncomplete() {
        assertEquals(List.of(3, 4), ids(response(results(5), 1, 2)));
        assertEquals(List.of(5), ids(response(results(5), 2, 2)));
    }

    @Test
    void pageBeyondTheEndIsEmpty() {
        SearchResponseDTO r = response(results(5), 3, 2);
        assertTrue(r.results().isEmpty());
        assertEquals(5, r.total());
        // page * size would overflow an int: still an empty page
        assertTrue(response(results(5), Integer.MAX_VALUE, Settings.MAX_PAGE_SIZE).results().isEmpty());
        // no result at all
        assertTrue(response(results(0), 0, 2).results().isEmpty());
    }

    @Test
    void recommendationsComeFromTheTopResultsOfThePage() {
        BookDTO recommended = BookDTO.from(new Book(42, "Recommended", "Author", "en", 100));
        when(recommendations.recommend(any(), any(), anyInt())).thenReturn(List.of(recommended));

        // page 1 of size 2: books 3 and 4, as sources and as excluded books
        SearchResponseDTO r = response(results(5), 1, 2);
        verify(recommendations).recommend(Set.of(3, 4), Set.of(3, 4), Settings.MAX_RECOMMENDATIONS);
        assertEquals(List.of(recommended), r.recommendations());
    }

    @Test
    void recommendationSourcesAreLimited() {
        // 7 results on the page: only the first RECOMMENDATION_SOURCES (5) are sources, all 7 are excluded
        response(results(7), 0, 7);
        verify(recommendations).recommend(Set.of(1, 2, 3, 4, 5), Set.of(1, 2, 3, 4, 5, 6, 7),
                Settings.MAX_RECOMMENDATIONS);
    }
}
