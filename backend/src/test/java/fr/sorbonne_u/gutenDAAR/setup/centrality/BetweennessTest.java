package fr.sorbonne_u.gutenDAAR.setup.centrality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Betweenness on small graphs whose values are computed by hand.
 * The matrices contain similarities: the cost of an edge is 1 - similarity.
 * The expected values are the raw counts divided by the number of pairs not containing the node, (n-1)(n-2)/2.
 */
class BetweennessTest {

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
        // 0 - 1 - 2 - 3: node 1 is on the paths {0,2} and {0,3} -> 2 pairs out of 3
        double[][] m = graph(4, new double[]{0, 1, 0.5}, new double[]{1, 2, 0.5}, new double[]{2, 3, 0.5});
        assertArrayEquals(new double[]{0, 2.0 / 3, 2.0 / 3, 0}, new Betweenness().compute(m), DELTA);
    }

    @Test
    void starCentreIsOnAllPaths() {
        // centre 0 and 4 leaves: the centre is on the paths of the C(4,2) = 6 pairs of leaves, out of 6
        double[][] m = graph(5, new double[]{0, 1, 0.5}, new double[]{0, 2, 0.5},
                new double[]{0, 3, 0.5}, new double[]{0, 4, 0.5});
        assertArrayEquals(new double[]{1, 0, 0, 0, 0}, new Betweenness().compute(m), DELTA);
    }

    @Test
    void twoEqualShortestPathsShareTheCredit() {
        // square 0 - 1 - 3 - 2 - 0, all costs 0.5: {0,3} has 2 shortest paths (via 1 or via 2), same for {1,2}.
        // each node gets 1/2 from one pair -> 0.5 out of 3 pairs
        double[][] m = graph(4, new double[]{0, 1, 0.5}, new double[]{1, 3, 0.5},
                new double[]{0, 2, 0.5}, new double[]{2, 3, 0.5});
        double each = 0.5 / 3;
        assertArrayEquals(new double[]{each, each, each, each}, new Betweenness().compute(m), DELTA);
    }

    @Test
    void longDirectEdgeIsNotUsed() {
        // direct edge 0 - 2 of cost 0.9, but 0 - 1 - 2 costs 0.2 + 0.2 = 0.4: node 1 is on {0,2}, the only pair
        double[][] m = graph(3, new double[]{0, 1, 0.8}, new double[]{1, 2, 0.8}, new double[]{0, 2, 0.1});
        assertArrayEquals(new double[]{0, 1, 0}, new Betweenness().compute(m), DELTA);
    }

    @Test
    void unreachablePairsCountForNothing() {
        // components 0 - 1 - 2 and 3 - 4: node 1 is only on {0,2}, out of (4*3)/2 = 6 pairs
        double[][] m = graph(5, new double[]{0, 1, 0.5}, new double[]{1, 2, 0.5}, new double[]{3, 4, 0.5});
        assertArrayEquals(new double[]{0, 1.0 / 6, 0, 0, 0}, new Betweenness().compute(m), DELTA);
    }

    @Test
    void tooSmallGraphs() {
        assertArrayEquals(new double[]{0, 0}, new Betweenness().compute(graph(2, new double[]{0, 1, 0.5})), DELTA);
        assertArrayEquals(new double[]{0}, new Betweenness().compute(new double[1][1]), DELTA);
    }
}
