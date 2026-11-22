package fr.sorbonne_u.gutenDAAR.setup;

import fr.sorbonne_u.gutenDAAR.database.entities.JaccardEdge;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DataSetup} unit tests (only for {@link DataSetup#keepNearestNeighbours(List, int)}).
 */
class DataSetupTest {

    /** The edges as "a-b" strings, the order of the returned list being unspecified. */
    private static Set<String> names(List<JaccardEdge> edges) {
        return edges.stream().map(e -> e.getBookA() + "-" + e.getBookB()).collect(Collectors.toSet());
    }

    /** Edges 1-2 (0.1), 2-3 (0.2), 3-4 (0.3), 1-3 (0.9). */
    private static List<JaccardEdge> edges() {
        return List.of(
                new JaccardEdge(1, 2, 0.1),
                new JaccardEdge(2, 3, 0.2),
                new JaccardEdge(3, 4, 0.3),
                new JaccardEdge(1, 3, 0.9));
    }

    @Test
    void keepsTheNearestNeighbourOfEachBook() {
        // nearest neighbour of 1: 2 (0.1) | of 2: 1 (0.1) | of 3: 2 (0.2) | of 4: 3 (0.3)
        // 3-4 is kept although 4 is not the nearest of 3, because 3 is the nearest of 4
        // 1-3 is the nearest of neither 1 nor 3: dropped
        assertEquals(Set.of("1-2", "2-3", "3-4"), names(DataSetup.keepNearestNeighbours(edges(), 1)));
    }

    @Test
    void keepsTwoNearestNeighbours() {
        // 2 nearest of 1: 2 (0.1), 3 (0.9): 1-3 is now kept
        assertEquals(Set.of("1-2", "2-3", "3-4", "1-3"), names(DataSetup.keepNearestNeighbours(edges(), 2)));
    }

    @Test
    void edgeKeptByBothBooksAppearsOnce() {
        // 1-2 is the nearest of 1 and of 2
        List<JaccardEdge> kept = DataSetup.keepNearestNeighbours(List.of(new JaccardEdge(1, 2, 0.5)), 1);
        assertEquals(1, kept.size());
    }

    @Test
    void kBiggerThanTheNeighboursKeepsEverything() {
        assertEquals(4, DataSetup.keepNearestNeighbours(edges(), 10).size());
    }

    @Test
    void noEdgeKept() {
        assertTrue(DataSetup.keepNearestNeighbours(edges(), 0).isEmpty());
        assertTrue(DataSetup.keepNearestNeighbours(List.of(), 3).isEmpty());
    }
}
