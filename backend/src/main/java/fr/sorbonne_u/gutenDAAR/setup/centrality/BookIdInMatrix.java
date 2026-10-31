package fr.sorbonne_u.gutenDAAR.setup.centrality;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * An adjacency matrix of books, with the position of each book in it.
 *
 * <p>
 * {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()} are overridden to consider the content
 * of the matrix: the methods generated for a record would only consider the reference of the array.
 * </p>
 *
 * @param bookIdToMatrixIndex   The row and column index in the matrix of each Gutenberg book id.
 * @param matrix                The square adjacency matrix: {@code matrix[y][x]} is the weight
 *                              of the link from the book of index y to the book of index x, 0 if there is no link.
 */
public record BookIdInMatrix(Map<Integer, Integer> bookIdToMatrixIndex, double[][] matrix) {

    @Override
    public boolean equals(Object o) {
        return o instanceof BookIdInMatrix(Map<Integer, Integer> otherBookIdToMatrixIndex, double[][] otherMatrix)
                && Objects.equals(bookIdToMatrixIndex, otherBookIdToMatrixIndex)
                && Arrays.deepEquals(matrix, otherMatrix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookIdToMatrixIndex, Arrays.deepHashCode(matrix));
    }

    @Override
    public String toString() {
        return "BookIdInMatrix[bookIdToMatrixIndex=" + bookIdToMatrixIndex
                + ", matrix=" + Arrays.deepToString(matrix) + "]";
    }
}
