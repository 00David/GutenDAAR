package fr.sorbonne_u.gutenDAAR.setup.centrality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Harmonic closeness centrality (Marchiori and Latora, 2000; Boldi and Vigna, 2014).
 *
 * <pre>
 *     H(v) = 1 / (n - 1) * Σ_{u ≠ v} 1 / d(v, u)
 * </pre>
 *
 * <p>
 * d(v, u) is the length of the shortest path from v to u in the graph, see {@link ShortestPaths}.
 * A book that cannot be reached from v adds 1 / ∞ = 0: unlike the classic closeness (n - 1) / Σ d(v, u),
 * the harmonic closeness stays defined when the graph is not connected.
 * </p>
 *
 * <p>
 * The values are in [0, 1 / MIN_COST]: the closer a book is to all the others, the higher its closeness.
 * </p>
 */
public class Closeness implements CentralityI {
    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(Closeness.class);

    /**
     * Compute the harmonic closeness of each node of the given graph.
     * @param adjacencyMatrix   The square adjacency matrix of the graph: {@code adjacencyMatrix[y][x]} is
     *                          the similarity (in ]0, 1]) of nodes y and x, 0 if there is no edge.
     * @return                  The harmonic closeness of each node, in the order of the matrix rows.
     */
    @Override
    public double[] compute(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        double[] closeness = new double[n];
        if (n < 2) return closeness; // a single node has no other node to be close to

        LOG.info("Closeness started for {} elements...", n);
        ShortestPaths shortestPaths = new ShortestPaths(adjacencyMatrix);

        for (int v = 0; v < n; v++) {
            double[] distances = shortestPaths.from(v);
            double sum = 0.0;
            for (int u = 0; u < n; u++) {
                // unreachable books (infinite distance) add 0
                if (u != v && distances[u] != Double.POSITIVE_INFINITY) sum += 1.0 / distances[u];
            }
            closeness[v] = sum / (n - 1);
        }
        LOG.info("Closeness done.");
        return closeness;
    }
}
