package fr.sorbonne_u.gutenDAAR.search;

import fr.sorbonne_u.gutenDAAR.Utils;
import fr.sorbonne_u.gutenDAAR.api.dto.BookDTOWithOccurrences;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import fr.sorbonne_u.gutenDAAR.database.services.BookService;
import fr.sorbonne_u.gutenDAAR.database.services.PostingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keyword search: the books whose index table contains the keyword.
 *
 * <p>Uses the inverted index (G1 seen from the word side): one lookup of the term in the postings table
 * returns the books containing it with their occurrences. Postgres is only used for this lookup;
 * the ranking is done here, so that it can later combine other weights (tf-idf, centrality).</p>
 *
 * <p>Ranking: according to the requested {@link RankingMethod}, either by number of occurrences
 * of the keyword in the book, or by a centrality, descending. Ties are broken by book id,
 * so that the order (and thus the pagination) is stable.</p>
 */
@Service
public class SearchService {

    /** The injected book service. */
    protected final BookService bookService;

    /** The injected posting service. */
    protected final PostingService postingService;

    public SearchService(PostingService postingService, BookService bookService) {
        this.postingService = postingService;
        this.bookService = bookService;
    }

    /**
     * Search the books containing a keyword, and rank them according to the given ranking method.
     * @param keyword           The keyword, a single word.
     * @param rankingMethod     The ranking method to apply.
     * @return                  Books matching the keyword with their occurrences of it, ranked by the ranking method.
     */
    public List<BookDTOWithOccurrences> searchByKeyword(String keyword, RankingMethod rankingMethod) {
        String term = Utils.normalize(keyword);

        // First get the postings of the term: each one associates a book id containing the term
        // with its number of occurrences in this book
        List<Posting> postings = postingService.findByTerm(term);
        if (postings.isEmpty()) {
            return new ArrayList<>();
        }

        // Then get the corresponding books
        List<Integer> bookIds = postings.stream().map(Posting::getBookId).distinct().toList();
        Map<Integer, Book> books = bookService.findAllById(bookIds);

        // Sort the books according to the ranking method
        List<Book> sortedBooks = rankingMethod == RankingMethod.occurrences
                ? RankingMethod.sortByOccurrences(postings, books, rankingMethod)
                : RankingMethod.sortByCentrality(books.values(), rankingMethod);

        // Finally, return the books as DTOs, with their occurrences of the term
        Map<Integer, Integer> occurrences = postings.stream()
                .collect(Collectors.toMap(Posting::getBookId, Posting::getOccurrences));
        return BookDTOWithOccurrences.from(sortedBooks, occurrences);
    }
}
