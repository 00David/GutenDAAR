package fr.sorbonne_u.gutenDAAR.search;

import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link RankingMethod} unit tests.
 */
class RankingMethodTest {

    /** Book with the given id and pagerank / closeness centralities ({@code null} if not computed). */
    private static Book book(int id, Double pagerank, Double closeness) {
        Book b = new Book(id, "Title " + id, "Author", "en", 100);
        if (pagerank != null) b.setPagerankCentrality(pagerank);
        if (closeness != null) b.setClosenessCentrality(closeness);
        return b;
    }

    private static List<Integer> ids(List<Book> books) {
        return books.stream().map(Book::getId).toList();
    }

    /** Books 1 to 4: pagerank 0.2, 0.5, null, 0.5 and closeness 3, 1, 2, null. */
    private static List<Book> books() {
        return List.of(book(1, 0.2, 3.0),
                book(2, 0.5, 1.0),
                book(3, null, 2.0),
                book(4, 0.5, null));
    }

    @Test
    void fromIgnoresCase() {
        assertEquals(RankingMethod.pagerank, RankingMethod.from("PageRank"));
        assertEquals(RankingMethod.occurrences, RankingMethod.from("OCCURRENCES"));
        assertEquals(RankingMethod.closeness, RankingMethod.from("closeness"));
        assertEquals(RankingMethod.betweenness, RankingMethod.from("Betweenness"));
        assertThrows(IllegalArgumentException.class, () -> RankingMethod.from("unknown"));
    }

    @Test
    void sortByCentralityDecreasingTiesByIdNullsLast() {
        // pagerank: 0.5 (books 2 and 4, tie broken by id), then 0.2 (book 1), then null (book 3)
        assertEquals(List.of(2, 4, 1, 3), ids(RankingMethod.sortByCentrality(books(), RankingMethod.pagerank)));
        // closeness: 3 (book 1), 2 (book 3), 1 (book 2), then null (book 4)
        assertEquals(List.of(1, 3, 2, 4), ids(RankingMethod.sortByCentrality(books(), RankingMethod.closeness)));
        // betweenness: never computed, so only the tie-break by id applies
        assertEquals(List.of(1, 2, 3, 4), ids(RankingMethod.sortByCentrality(books(), RankingMethod.betweenness)));
    }

    @Test
    void sortByCentralityLeavesTheInputUnchanged() {
        List<Book> input = new ArrayList<>(books());
        RankingMethod.sortByCentrality(input, RankingMethod.pagerank);
        assertEquals(List.of(1, 2, 3, 4), ids(input));
    }

    @Test
    void sortInPlaceByCentralitySortsTheGivenList() {
        List<Book> input = new ArrayList<>(books());
        RankingMethod.sortInPlaceByCentrality(input, RankingMethod.pagerank);
        assertEquals(List.of(2, 4, 1, 3), ids(input));
    }

    @Test
    void sortByOccurrencesDecreasingTiesById() {
        Map<Integer, Book> books = books().stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        List<Posting> postings = List.of(
                new Posting(3, "whale", 5),
                new Posting(1, "whale", 5),
                new Posting(2, "whale", 9),
                new Posting(4, "whale", 1));
        // 9 (book 2), 5 (books 1 and 3, tie broken by id), 1 (book 4)
        assertEquals(List.of(2, 1, 3, 4),
                ids(RankingMethod.sortByOccurrences(postings, books, RankingMethod.occurrences)));
    }

    @Test
    void wrongRankingMethodIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RankingMethod.sortByCentrality(books(), RankingMethod.occurrences));
        assertThrows(IllegalArgumentException.class,
                () -> RankingMethod.sortByOccurrences(List.of(), Map.of(), RankingMethod.pagerank));
    }
}
