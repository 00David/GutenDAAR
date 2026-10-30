package fr.sorbonne_u.gutenDAAR.setup.centrality;

/**
 * A centrality measure, computed on a directed graph given as an adjacency matrix.
 */
public interface CentralityI {
    /**
     * Compute the centrality of each node of the given graph.
     * @param adjacencyMatrix   The square adjacency matrix of the graph: {@code adjacencyMatrix[y][x]} is
     *                          the weight of the link from node y to node x, 0 if there is no link.
     * @return                  The centrality of each node, in the order of the matrix rows.
     */
    double[] compute(double[][] adjacencyMatrix);
}
