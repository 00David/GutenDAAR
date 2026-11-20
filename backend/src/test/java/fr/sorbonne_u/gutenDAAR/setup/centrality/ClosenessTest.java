package fr.sorbonne_u.gutenDAAR.setup.centrality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Harmonic closeness on small graphs whose values are computed by hand.
 * The matrices contain similarities: the cost of an edge is 1 - similarity.
 */
class ClosenessTest {

    private static final double DELTA = 1e-12;

    /** Undirected graph from the given edges (node a, node b, similarity). */
    private static double[][] graph(int n, double[]... edges) {
        double[][] m = new double[n][n];
        for (double[] e : edges) {
            m[(int) e[0]][(int) e[1]] = e[2];
            m[(int) e[1]][(int) e[0]] = e[2];
        }
        return m;
    }

    @Test
    void pathOfFourNodes() {
        // 0 - 1 - 2 - 3, every edge of cost 0.5
        double[][] m = graph(4, new double[]{0, 1, 0.5}, new double[]{1, 2, 0.5}, new double[]{2, 3, 0.5});
        // end node 0: distances 0.5, 1, 1.5 -> (2 + 1 + 2/3) / 3
        // inner node 1: distances 0.5, 0.5, 1 -> (2 + 2 + 1) / 3
        double end = (2 + 1 + 2.0 / 3) / 3;
        double inner = 5.0 / 3;
        assertArrayEquals(new double[]{end, inner, inner, end}, new Closeness().compute(m), DELTA);
    }

    @Test
    void shortestPathThroughAnotherNodeIsUsed() {
        // direct edge 0 - 2 of cost 0.9, but 0 - 1 - 2 costs 0.2 + 0.2 = 0.4
        double[][] m = graph(3, new double[]{0, 1, 0.8}, new double[]{1, 2, 0.8}, new double[]{0, 2, 0.1});
        // node 0: distances 0.2 (to 1), 0.4 (to 2) -> (5 + 2.5) / 2
        assertEquals((5 + 2.5) / 2, new Closeness().compute(m)[0], DELTA);
    }

    @Test
    void unreachableNodesAddZero() {
        // two components: 0 - 1 (cost 0.5) and 2 - 3 (cost 0.25)
        double[][] m = graph(4, new double[]{0, 1, 0.5}, new double[]{2, 3, 0.75});
        // node 0 only reaches 1: (1 / 0.5) / 3
        assertArrayEquals(new double[]{2.0 / 3, 2.0 / 3, 4.0 / 3, 4.0 / 3}, new Closeness().compute(m), DELTA);
    }

    @Test
    void isolatedNodeAndSingleNode() {
        assertArrayEquals(new double[]{0, 0}, new Closeness().compute(new double[2][2]), DELTA);
        assertArrayEquals(new double[]{0}, new Closeness().compute(new double[1][1]), DELTA);
    }
}
