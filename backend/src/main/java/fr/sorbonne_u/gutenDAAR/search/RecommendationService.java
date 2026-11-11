package fr.sorbonne_u.gutenDAAR.search;

import fr.sorbonne_u.gutenDAAR.api.dto.BookDTO;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.JaccardEdge;
import fr.sorbonne_u.gutenDAAR.database.services.BookService;
import fr.sorbonne_u.gutenDAAR.database.services.JaccardEdgeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recommendations: books that are neighbours, in the document graph (G2, jaccard edges),
 * of the top-ranked results of a search.
 *
 * <p>Algorithm:</p>
 * <ol>
 *     <li>get every edge leaving the top results (one lookup on the primary key of jaccard_edges);</li>
 *     <li>drop the books already in the result list (spec: recommendations must be other books);</li>
 *     <li>keep, for each candidate, its smallest distance to one of the top results;</li>
 *     <li>sort by that distance (closest first, ties by book id) and keep the first ones;</li>
 *     <li>load these books (one lookup on the primary key of books).</li>
 * </ol>
 *
 * <p>The edges are precomputed offline, so this costs a few milliseconds and is sent in the same
 * response as the search results.</p>
 */
@Service
public class RecommendationService {

    /** The injected jaccard edge service. */
    protected final JaccardEdgeService jeService;

    /** The injected book service. */
    protected final BookService bookService;

    public RecommendationService(JaccardEdgeService jeService, BookService bookService) {
        this.jeService = jeService;
        this.bookService = bookService;
    }

    /**
     * Recommend books close to the given top results.
     * @param topBookIds        The top-ranked results, whose neighbours are wanted.
     * @param excludedBookIds   The books never to recommend (all the search results).
     * @param limit             The maximum number of recommendations.
     * @return                  The recommended books, the closest first.
     */
    public List<BookDTO> recommend(Set<Integer> topBookIds, Set<Integer> excludedBookIds, int limit) {
        if (topBookIds.isEmpty() || limit <= 0) {
            return List.of();
        }

        // Smallest distance from each candidate to one of the top results
        Map<Integer, Double> bestDistance = new HashMap<>();
        for (JaccardEdge edge : jeService.findEdgesFrom(topBookIds)) {
            int candidate = edge.getBookB();
            if (!excludedBookIds.contains(candidate)) {
                bestDistance.merge(candidate, edge.getDistance(), Math::min);
            }
        }

        List<Map.Entry<Integer, Double>> candidates = new ArrayList<>(bestDistance.entrySet());
        candidates.sort(Map.Entry.<Integer, Double>comparingByValue()
                .thenComparing(Map.Entry.comparingByKey()));

        List<Integer> recommendedIds = new ArrayList<>(Math.min(limit, candidates.size()));
        for (int i = 0; i < candidates.size() && i < limit; i++) {
            recommendedIds.add(candidates.get(i).getKey());
        }

        // Load the recommended books, keeping the distance order
        Map<Integer, Book> books = bookService.findAllById(recommendedIds);
        List<BookDTO> recommendedBooks = new ArrayList<>(recommendedIds.size());
        for (int id : recommendedIds) {
            Book book = books.get(id);
            if (book != null) {
                recommendedBooks.add(BookDTO.from(book));
            }
        }
        return recommendedBooks;
    }
}
