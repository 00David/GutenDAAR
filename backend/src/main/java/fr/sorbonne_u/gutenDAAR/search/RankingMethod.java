package fr.sorbonne_u.gutenDAAR.search;

import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Indicate possible ranking methods.
 */
public enum RankingMethod {
    /**
     * Rank by the decreasing number of {@link SearchMode#keyword} occurrences.
     * Does not apply to other search modes.
     */
    occurrences,
    /** Rank by the decreasing closeness centrality. */
    closeness,
    /** Rank by the decreasing betweenness centrality. */
    betweenness,
    /** Rank by the decreasing pagerank (centrality). */
    pagerank;

    /**
     * Convert a given ranking method (as a string) to the equivalent enumeration.
     * @param methodString                  A ranking method.
     * @return                              Its equivalent as aan enumeration.
     * @throws IllegalArgumentException     If not existing ranking enumeration matches the given string.
     */
    public static RankingMethod from(String methodString) {
        String lowerMethodString = methodString.toLowerCase(Locale.ROOT);
        return switch (lowerMethodString) {
            case "occurrences" -> occurrences;
            case "pagerank" -> pagerank;
            case "closeness" -> closeness;
            case "betweenness" -> betweenness;
            default -> throw new IllegalArgumentException("Unknown ranking method: " + methodString);
        };
    }

    /**
     * Rank the books containing a keyword by decreasing number of occurrences of this keyword.
     *
     * <p>Ties are broken by book id, so that the order (and thus the pagination) is stable.</p>
     *
     * @param postings                      The postings of the keyword (one per book containing it).
     * @param books                         The books of these postings, by id. Postings whose book is absent are ignored.
     * @param rankingMethod                 The ranking method, which must be {@link #occurrences}.
     * @return                              A new (modifiable) list of the books, ranked by occurrences.
     * @throws IllegalArgumentException     If the ranking method is not {@link #occurrences}:
     *                                      use {@link #sortByCentrality(Collection, RankingMethod)} instead.
     */
    public static List<Book> sortByOccurrences(Collection<Posting> postings, Map<Integer, Book> books,
                                               RankingMethod rankingMethod) {
        if (rankingMethod != occurrences) {
            throw new IllegalArgumentException(rankingMethod + " is not the occurrences ranking method.");
        }
        return postings.stream()
                .sorted(Comparator.comparingInt(Posting::getOccurrences).reversed()
                        .thenComparingInt(Posting::getBookId))
                .map(p -> books.get(p.getBookId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Rank books by decreasing centrality,
     * the centrality being the one of the given ranking method.
     * Books whose centrality is not computed yet ({@code null}) come last.
     *
     * <p>Ties are broken by book id, so that the order (and thus the pagination) is stable.</p>
     *
     * @param books                         The books to rank (left unchanged).
     * @param rankingMethod                 The centrality ranking method to apply.
     * @return                              A new (modifiable) list of the books, ranked by centrality.
     * @throws IllegalArgumentException     If the ranking method is {@link #occurrences}, which is not a centrality:
     *                                      use {@link #sortByOccurrences(Collection, Map, RankingMethod)} instead.
     */
    public static List<Book> sortByCentrality(Collection<Book> books, RankingMethod rankingMethod) {
        List<Book> sorted = new ArrayList<>(books);
        sortInPlaceByCentrality(sorted, rankingMethod);
        return sorted;
    }

    /**
     * Rank books by decreasing centrality,
     * the centrality being the one of the given ranking method.
     * Books whose centrality is not computed yet ({@code null}) come last.
     *
     * <p>Ties are broken by book id, so that the order (and thus the pagination) is stable.</p>
     *
     * @param books                         The books to rank (MODIFIABLE LIST, sorted after calling the method).
     * @param rankingMethod                 The centrality ranking method to apply.
     * @throws IllegalArgumentException     If the ranking method is {@link #occurrences}, which is not a centrality:
     *                                      use {@link #sortByOccurrences(Collection, Map, RankingMethod)} instead.
     */
    public static void sortInPlaceByCentrality(List<Book> books, RankingMethod rankingMethod) {
        Function<Book, @Nullable Double> centrality = switch (rankingMethod) {
            case pagerank -> Book::getPagerankCentrality;
            case closeness -> Book::getClosenessCentrality;
            case betweenness -> Book::getBetweennessCentrality;
            case occurrences -> throw new IllegalArgumentException(
                    "occurrences is not a centrality: use sortByOccurrences instead.");
        };
        books.sort(Comparator.comparing(centrality, Comparator.nullsLast(Comparator.<Double>reverseOrder()))
                .thenComparingInt(Book::getId));
    }
}
