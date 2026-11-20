package fr.sorbonne_u.gutenDAAR.setup.centrality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Betweenness centrality, computed by Brandes' algorithm on a weighted undirected graph.
 *
 * <pre>
 *     B(v) = Σ_{s ≠ v ≠ t} σ_st(v) / σ_st
 * </pre>
 *
 * <p>
 * σ_st is the number of shortest paths from s to t, and σ_st(v) the number of them going through v:
 * a book is central if it lies on many shortest paths between other books (a "bridge").
 * </p>
 *
 * <p>
 * For each source s, one Dijkstra run gives the shortest paths from s ({@link ShortestPaths#allFrom(int)}),
 * then the dependencies δ_s(v) = Σ_{t} σ_st(v) / σ_st are accumulated from the farthest nodes back to s:
 * δ_s(v) = Σ_{w : v ∈ P(w)} σ_sv / σ_sw * (1 + δ_s(w)). Cost: O(n m + n² log n) instead of enumerating all the paths.
 * </p>
 *
 * <p>
 * The graph is undirected, so each pair {s, t} is counted twice (from s and from t): the sum is divided by 2.
 * It is then normalized by the number of pairs not containing v, (n - 1)(n - 2) / 2, so that the values are in [0, 1]
 * and comparable between libraries of different sizes.
 * </p>
 */
public class Betweenness implements CentralityI {
    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(Betweenness.class);

    /**
     * Compute the normalized betweenness of each node of the given graph.
     * @param adjacencyMatrix   The square adjacency matrix of the graph: {@code adjacencyMatrix[y][x]} is
     *                          the similarity (in ]0, 1]) of nodes y and x, 0 if there is no edge.
     *                          It must be symmetric.
     * @return                  The normalized betweenness of each node, in the order of the matrix rows.
     */
    @Override
    public double[] compute(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        double[] betweenness = new double[n];
        if (n < 3) return betweenness; // no pair of other nodes to stand between

        LOG.info("Betweenness started for {} elements...", n);
        ShortestPaths shortestPaths = new ShortestPaths(adjacencyMatrix);

        for (int s = 0; s < n; s++) {
            ShortestPaths.SingleSource paths = shortestPaths.allFrom(s);
            double[] counts = paths.counts();
            List<List<Integer>> predecessors = paths.predecessors();
            int[] order = paths.order();
            double[] dependencies = new double[n];

            // From the farthest node back to the source: each node passes its dependency to its predecessors,
            // shared according to the number of shortest paths going through each of them.
            for (int i = order.length - 1; i >= 0; i--) {
                int w = order[i];
                for (int v : predecessors.get(w)) {
                    dependencies[v] += counts[v] / counts[w] * (1.0 + dependencies[w]);
                }
                if (w != s) betweenness[w] += dependencies[w];
            }
        }

        double pairsWithoutV = (n - 1.0) * (n - 2.0) / 2.0;
        for (int v = 0; v < n; v++) {
            // each pair {s, t} counted twice (from s and from t): undirected graph
            betweenness[v] /= 2.0;
            // normalized by the (n - 1)(n - 2) / 2 pairs not containing v: values in [0, 1]
            betweenness[v] /= pairsWithoutV;
        }

        LOG.info("Betweenness done.");
        return betweenness;
    }
}
