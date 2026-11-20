package fr.sorbonne_u.gutenDAAR.setup.centrality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Shortest paths in a weighted graph given as an adjacency matrix of similarities (the jaccard graph).
 *
 * <p>
 * The cost of the edge between two nodes is their distance: 1 - similarity. The more similar two books are,
 * the shorter their edge. A similarity of 0 means there is no edge.
 * </p>
 *
 * <p>
 * The distances are computed by Dijkstra's algorithm, run from one source at a time:
 * O(m log n) per source with a binary heap, so O(n m log n) for all the sources. On our sparse k nearest neighbours
 * graph (m ≈ k n), this is much cheaper than Floyd-Warshall's O(n³).
 * </p>
 */
public class ShortestPaths {

    /**
     * Minimal cost of an edge. Two books with the exact same vocabulary have a jaccard distance of 0:
     * their cost is raised to this value, so that every edge has a strictly positive cost
     * (needed by the harmonic closeness, 1 / distance, and by Brandes' algorithm).
     */
    public static final double MIN_COST = 1e-9;

    /** The neighbours of each node. */
    protected final int[][] neighbours;
    /** The cost of each edge: {@code costs[y][i]} is the cost of the edge from y to {@code neighbours[y][i]}. */
    protected final double[][] costs;

    /**
     * Build the adjacency lists of the given graph, so that each Dijkstra run only visits the existing edges.
     * @param adjacencyMatrix   The square adjacency matrix of the graph: {@code adjacencyMatrix[y][x]} is
     *                          the similarity (in ]0, 1]) of nodes y and x, 0 if there is no edge.
     */
    public ShortestPaths(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        neighbours = new int[n][];
        costs = new double[n][];

        for (int y = 0; y < n; y++) {
            List<Integer> ys = new ArrayList<>();
            for (int x = 0; x < n; x++) {
                if (x != y && adjacencyMatrix[y][x] != 0.0) ys.add(x);
            }
            neighbours[y] = new int[ys.size()];
            costs[y] = new double[ys.size()];
            for (int i = 0; i < ys.size(); i++) {
                int x = ys.get(i);
                neighbours[y][i] = x;
                costs[y][i] = Math.max(1.0 - adjacencyMatrix[y][x], MIN_COST);
            }
        }
    }

    /**
     * @return  The number of nodes of the graph.
     */
    public int size() {
        return neighbours.length;
    }

    /**
     * Compute the length of the shortest path from the given source to every node, with Dijkstra's algorithm.
     * @param source    The index of the source node.
     * @return          The distance from the source to each node: 0 for the source itself,
     *                  {@link Double#POSITIVE_INFINITY} for a node that cannot be reached.
     */
    public double[] from(int source) {
        return allFrom(source).distances();
    }

    /**
     * The shortest paths from one source, as needed by Brandes' algorithm.
     * @param distances     The distance from the source to each node, {@link Double#POSITIVE_INFINITY} if unreachable.
     * @param counts        The number of shortest paths from the source to each node (σ), 0 if unreachable.
     * @param predecessors  The predecessors of each node on its shortest paths from the source.
     * @param order         The reached nodes, by non-decreasing distance from the source (the source first).
     */
    public record SingleSource(double[] distances, double[] counts, List<List<Integer>> predecessors, int[] order) {}

    /**
     * Compute the shortest paths from the given source to every node, with Dijkstra's algorithm,
     * also counting them and keeping their predecessors (Brandes, 2001).
     *
     * <p>
     * Two paths are counted as equally short only if their lengths are exactly equal: with real-valued
     * jaccard distances, two different paths almost never have the same length, so most counts are 1.
     * </p>
     *
     * @param source    The index of the source node.
     * @return          The distances, number of shortest paths, predecessors and order of the reached nodes.
     */
    public SingleSource allFrom(int source) {
        int n = size();
        double[] distances = new double[n];
        Arrays.fill(distances, Double.POSITIVE_INFINITY);
        distances[source] = 0.0;
        double[] counts = new double[n];
        counts[source] = 1.0;
        List<List<Integer>> predecessors = new ArrayList<>(n);
        for (int i = 0; i < n; i++) predecessors.add(new ArrayList<>());
        boolean[] settled = new boolean[n];
        int[] order = new int[n];
        int reached = 0;

        // Nodes to visit, closest first: (distance, node). A node can be queued several times,
        // the outdated entries (longer than its known distance) are skipped.
        PriorityQueue<double[]> queue = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        queue.add(new double[]{0.0, source});

        while (!queue.isEmpty()) {
            double[] entry = queue.poll();
            double distance = entry[0];
            int y = (int) entry[1];
            if (settled[y] || distance > distances[y]) continue; // outdated entry
            settled[y] = true;
            order[reached++] = y;

            for (int i = 0; i < neighbours[y].length; i++) {
                int x = neighbours[y][i];
                double throughY = distance + costs[y][i];
                if (throughY < distances[x]) {
                    // strictly shorter path: y becomes the only predecessor of x
                    distances[x] = throughY;
                    counts[x] = counts[y];
                    predecessors.get(x).clear();
                    predecessors.get(x).add(y);
                    queue.add(new double[]{throughY, x});
                } else if (throughY == distances[x]) {
                    // another shortest path, through y
                    counts[x] += counts[y];
                    predecessors.get(x).add(y);
                }
            }
        }
        return new SingleSource(distances, counts, predecessors, Arrays.copyOf(order, reached));
    }
}
