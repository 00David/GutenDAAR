package fr.sorbonne_u.gutenDAAR.search;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.Utils;
import fr.sorbonne_u.gutenDAAR.api.dto.BookSearchedDTO;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.BookText;
import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import fr.sorbonne_u.gutenDAAR.database.services.BookService;
import fr.sorbonne_u.gutenDAAR.database.services.BookTextService;
import fr.sorbonne_u.gutenDAAR.database.services.PostingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for applying each {#link {@link SearchMode}.
 */
@Service
public class SearchService {
    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(SearchService.class);

    /** The injected book service. */
    protected final BookService bookService;

    /** The injected book text service. */
    protected final BookTextService bookTextService;

    /** The injected posting service. */
    protected final PostingService postingService;

    public SearchService(PostingService postingService, BookTextService bookTextService, BookService bookService) {
        this.postingService = postingService;
        this.bookTextService = bookTextService;
        this.bookService = bookService;
    }

    /**
     * Search the books containing a keyword, and rank them according to the given ranking method.
     * @param keyword           The keyword, a single word.
     * @param rankingMethod     The ranking method to apply.
     * @return                  Books matching the keyword with their occurrences of it, ranked by the ranking method.
     */
    public List<BookSearchedDTO> searchByKeyword(String keyword, RankingMethod rankingMethod) {
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

        List<BookSearchedDTO> results = BookSearchedDTO.from(sortedBooks, occurrences);
        LOG.atInfo()
                .addArgument(results::size)
                .addArgument(keyword)
                .log("Found {} books after '{}' keyword search.");
        return results;
    }

    /**
     * Search the books containing an indexed term matching the pattern, and rank them according to the given ranking method.
     * @param regex             The regex pattern.
     * @param rankingMethod     The ranking method to apply.
     * @return                  Books (using their indexed terms) matching the regex, ranked by the ranking method.
     */
    public List<BookSearchedDTO> searchByRegex(Pattern regex, RankingMethod rankingMethod) {
        // First get all books
        List<Book> books = bookService.findAll();

        // Then, filter them by keeping those with term(s) matching the given regex
        List<Book> matchingBooks = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            Stream<Posting> bookPostings = postingService.findByBookId(b.getId()).stream();
            if (bookPostings.anyMatch(
                    p -> regex.matcher(p.getTerm()).matches()
            )) {
                matchingBooks.add(b);
            }

            int done = i + 1;
            if (done % Settings.PROGRESS_BAR_STEP == 0) {
                LOG.atInfo()
                        .addArgument(() -> done)
                        .addArgument(books::size)
                        .log("Regex search: {}/{} books scanned.");
            }
        }

        // Sort the books according to the ranking method
        RankingMethod.sortByCentrality(matchingBooks, rankingMethod);

        // Finally, return the books as DTOs (with their occurrences not provided)
        List<BookSearchedDTO> results = BookSearchedDTO.from(matchingBooks);
        LOG.atInfo()
                .addArgument(results::size)
                .addArgument(regex::toString)
                .log("Found {} books after '{}' regex search.");
        return results;
    }

    /**
     * Search the books containing a word matching the pattern, and rank them according to the given ranking method.
     * @param regex             The regex pattern.
     * @param rankingMethod     The ranking method to apply.
     * @return                  Books (using their words) matching the regex, ranked by the ranking method.
     */
    public List<BookSearchedDTO> searchByRegexFullText(Pattern regex, RankingMethod rankingMethod) {
        // First get all books
        List<Book> books = bookService.findAll();

        // Then, filter them by keeping those with word(s) matching the given regex
        List<Book> matchingBooks = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            BookText bookText = bookTextService.findById(b.getId()).get();
            Stream<String> bookLines = bookText.getContent().lines();
            if (bookLines.anyMatch(l -> regex.matcher(l).find())) {
                matchingBooks.add(b);
            }

            int done = i + 1;
            if (done % Settings.PROGRESS_BAR_STEP == 0) {
                LOG.atInfo()
                        .addArgument(() -> done)
                        .addArgument(books::size)
                        .log("Regex FULL search: {}/{} books scanned.");
            }
        }

        // Sort the books according to the ranking method
        RankingMethod.sortByCentrality(matchingBooks, rankingMethod);

        // Finally, return the books as DTOs (with their occurrences not provided)
        List<BookSearchedDTO> results = BookSearchedDTO.from(matchingBooks);
        LOG.atInfo()
                .addArgument(results::size)
                .addArgument(regex::toString)
                .log("Found {} books after '{}' regex FULL search.");
        return results;
    }
}
