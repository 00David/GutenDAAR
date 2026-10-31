package fr.sorbonne_u.gutenDAAR.setup.centrality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PageRank centrality, computed by power iteration.
 *
 * <p>
 * At each step, a random surfer either follows one of the outgoing links of its current node,
 * chosen proportionally to its weight, or teleports to a uniformly chosen node with probability {@link #tpProbability}.
 * The rank of a node is the probability of finding the surfer on it, once this distribution is stationary.
 * </p>
 */
public class PageRank implements CentralityI {
    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(PageRank.class);

    /** The probability of teleporting to a uniformly chosen node, instead of following a link. */
    protected double tpProbability;
    /** The computation stops when the distance between two successive rank vectors falls below this value. */
    protected double epsilon;
    /** The maximum number of iterations, {@code null} for no limit. */
    protected Integer maxIterations;

    public PageRank() {
        this(0.15, 1e-10, null);
    }

    public PageRank(double tpProbability, double epsilon, Integer maxIterations) {
        this.tpProbability = tpProbability;
        this.epsilon = epsilon;
        this.maxIterations = maxIterations;
    }

    public double getTpProbability() {
        return tpProbability;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public Integer getMaxIterations() {
        return maxIterations;
    }

    public void setTpProbability(double tpProbability) {
        this.tpProbability = tpProbability;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public void setMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Compute the PageRank of each node of the given graph, iterating until
     * the ranks converge according to {@link #epsilon} or {@link #maxIterations} is reached.
     * @param adjacencyMatrix   The square adjacency matrix of the graph: {@code adjacencyMatrix[y][x]} is
     *                          the positive weight of the link from node y to node x, 0 if there is no link.
     *                          The bigger the weight, the more rank the link transmits.
     * @return                  The rank of each node, in the order of the matrix rows. The ranks sum to 1.
     */
    @Override
    public double[] compute(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;

        double[] previousRanks = initRanks(n);
        double[] ranks = previousRanks;

        double[][] transitions = initTransitionMatrix(adjacencyMatrix);

        double delta = Double.MAX_VALUE;
        int iterations  = 0;

        LOG.info("PageRank started for {} elements...", n);
        while (delta >= epsilon && (maxIterations == null || iterations < maxIterations)) {
            previousRanks = ranks;

            ranks = product(transitions, previousRanks);
            multiplyByTPProbability(ranks);
            addUniformTPProbability(ranks);
            normalize(ranks);

            delta = computeDelta(ranks, previousRanks);
            iterations++;

            LOG.debug("PageRank iteration {} done, delta = {}.", iterations, delta);
        }
        LOG.info("PageRank done in {} iterations.", iterations);
        return ranks;
    }

    /**
     * Build the initial ranks, uniformly distributed over the nodes.
     * @param size      The number of nodes.
     * @return          The initial rank of each node.
     */
    protected static double[] initRanks(int size) {
        double[] ranks = new double[size];
        double uniformProba = 1.0 / size;
        for (int i = 0; i < size; i++) ranks[i] = uniformProba;
        return ranks;
    }

    /**
     * Build the transition matrix of the graph: {@code transitionMatrix[x][y]} is the probability of going
     * from node y to node x by following a link, so the weight of this link / the total weight of y's outgoing links.
     *
     * <p>The column of a node without outgoing link is left empty.</p>
     *
     * @param adjacencyMatrix   The square adjacency matrix of the graph.
     * @return                  The transition matrix, of the same shape.
     */
    protected static double[][] initTransitionMatrix(double[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        double[][] transitionMatrix = new double[n][n];

        for (int y = 0; y < adjacencyMatrix.length; y++) {
            double outDegree = 0.0;
            for (int x = 0; x < adjacencyMatrix.length; x++) outDegree += adjacencyMatrix[y][x];

            if (outDegree != 0) {
                for (int x = 0; x < adjacencyMatrix.length; x++) {
                    if (adjacencyMatrix[y][x] != 0.0) { // if there is a link from y to x
                        transitionMatrix[x][y] = adjacencyMatrix[y][x] / outDegree;
                    }
                }
            }
        }
        return transitionMatrix;
    }

    /**
     * Compute the product of the given square matrix by the given vector.
     * @param matrix                        The square matrix.
     * @param vector                        The vector.
     * @return                              The resulting vector.
     * @throws IllegalArgumentException     If the matrix rows and the vector do not have the same length.
     */
    protected static double[] product(double[][] matrix, double[] vector) {
        double[] result = new double[vector.length];
        for (int y = 0; y < matrix.length; y++) {
            if (matrix[y].length != vector.length) {
                throw new IllegalArgumentException("Matrix shape does not match with vector length.");
            }
            for (int x = 0; x < matrix.length; x++) {
                result[y] = result[y] + matrix[y][x] * vector[x];
            }
        }
        return result;
    }

    /**
     * Weight the given ranks, in place, by the probability of following a link: 1 - {@link #tpProbability}.
     * @param vector    The ranks obtained by following the links.
     */
    protected void multiplyByTPProbability(double[] vector) {
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] * (1.0 - tpProbability);
        }
    }

    /**
     * Add to the given ranks, in place, the probability of teleporting to each node:
     * {@link #tpProbability} / (number of nodes).
     * @param vector    The ranks, already weighted by the probability of following a link.
     */
    protected void addUniformTPProbability(double[] vector) {
        double uniformProba = 1.0 / vector.length;
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] + (tpProbability * uniformProba);
        }
    }

    /**
     * Normalize the given vector in place, so that the sum of its absolute values is 1.
     *
     * <p>
     * This also gives back the rank lost by the nodes without outgoing link,
     * shared between all the nodes proportionally to their rank.
     * </p>
     *
     * @param vector    The vector to normalize.
     */
    protected static void normalize(double[] vector) {
        double total = 0.0;
        for (int i = 0; i < vector.length; i++) total += Math.abs(vector[i]);
        if (total != 0.0) {
            for (int i = 0; i < vector.length; i++) vector[i] = vector[i] / total;
        }
    }

    /**
     * Compute the distance between the two given vectors.
     * @param vector1                       The first vector.
     * @param vector2                       The second vector.
     * @return                              The sum of the absolute differences between their components.
     * @throws IllegalArgumentException     If the vectors do not have the same length.
     */
    protected static double computeDelta(double[] vector1, double[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same length.");
        }
        double delta = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            delta += Math.abs(vector1[i] - vector2[i]);
        }
        return delta;
    }
}
