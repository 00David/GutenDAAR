package fr.sorbonne_u.gutenDAAR.setup.centrality;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PageRank on small graphs whose values are computed by hand.
 * A teleportation probability of 0.5 is used, so that the stationary ranks are simple fractions:
 * rank(x) = 0.5 / n + 0.5 * (sum over the links y -> x of rank(y) * weight(y -> x) / outWeight(y)).
 */
class PageRankTest {

    private static final double DELTA = 1e-9;

    /** PageRank with a teleportation probability of 0.5, iterating until convergence. */
    private static PageRank pageRank() {
        return new PageRank(0.5, 1e-12, null);
    }

    /** Undirected graph from the given edges (node a, node b, weight). */
    private static double[][] graph(int n, double[]... edges) {
        double[][] m = new double[n][n];
        for (double[] e : edges) {
            m[(int) e[0]][(int) e[1]] = e[2];
            m[(int) e[1]][(int) e[0]] = e[2];
        }
        return m;
    }

    @Test
    void symmetricGraphGivesUniformRanks() {
        // cycle 0 - 1 - 2 - 3 - 0: every node plays the same role
        double[][] m = graph(4, new double[]{0, 1, 1}, new double[]{1, 2, 1},
                new double[]{2, 3, 1}, new double[]{3, 0, 1});
        assertArrayEquals(new double[]{0.25, 0.25, 0.25, 0.25}, pageRank().compute(m), DELTA);
    }

    @Test
    void starCenterHasTheHighestRank() {
        // center 0 linked to the leaves 1, 2, 3
        double[][] m = graph(4, new double[]{0, 1, 1}, new double[]{0, 2, 1}, new double[]{0, 3, 1});
        // center: c = 0.125 + 0.5 * (3 leaves * l * 1)  and  c + 3l = 1  ->  c = 0.125 + 0.5 * (1 - c)  ->  c = 5/12
        // leaf:   l = (1 - 5/12) / 3 = 7/36
        double c = 5.0 / 12;
        double l = 7.0 / 36;
        assertArrayEquals(new double[]{c, l, l, l}, pageRank().compute(m), DELTA);
    }

    @Test
    void heavierLinkTransmitsMoreRank() {
        // 0 - 1 of weight 3, 0 - 2 of weight 1: node 0 sends 3/4 of its rank to 1, 1/4 to 2
        double[][] m = graph(3, new double[]{0, 1, 3}, new double[]{0, 2, 1});
        // node 0 (like a star center): c = 1/6 + 0.5 * (1 - c)  ->  c = 4/9
        // node 1: 1/6 + 0.5 * 3/4 * 4/9 = 1/6 + 1/6 = 1/3
        // node 2: 1/6 + 0.5 * 1/4 * 4/9 = 1/6 + 1/18 = 2/9
        assertArrayEquals(new double[]{4.0 / 9, 1.0 / 3, 2.0 / 9}, pageRank().compute(m), DELTA);
    }

    @Test
    void graphWithoutLinksGivesUniformRanks() {
        // only teleportation: 0.5 / 3 for each node, then normalized to 1/3
        double third = 1.0 / 3;
        assertArrayEquals(new double[]{third, third, third}, pageRank().compute(new double[3][3]), DELTA);
        assertArrayEquals(new double[]{1}, pageRank().compute(new double[1][1]), DELTA);
    }

    @Test
    void maxIterationsStopsTheComputation() {
        double[][] star = graph(4, new double[]{0, 1, 1}, new double[]{0, 2, 1}, new double[]{0, 3, 1});

        // no iteration: the initial uniform ranks
        assertArrayEquals(new double[]{0.25, 0.25, 0.25, 0.25},
                new PageRank(0.5, 1e-12, 0).compute(star), DELTA);

        // a single iteration from 1/4 each:
        // center: 0.125 + 0.5 * (3 * 1/4) = 1/2
        // leaf:   0.125 + 0.5 * (1/4 / 3) = 1/6
        double sixth = 1.0 / 6;
        assertArrayEquals(new double[]{0.5, sixth, sixth, sixth},
                new PageRank(0.5, 1e-12, 1).compute(star), DELTA);
    }

    @Test
    void defaultRanksSumToOne() {
        double[][] m = graph(5, new double[]{0, 1, 0.2}, new double[]{1, 2, 0.9},
                new double[]{2, 0, 0.4}, new double[]{3, 4, 0.7});
        assertEquals(1.0, Arrays.stream(new PageRank().compute(m)).sum(), DELTA);
    }
}
