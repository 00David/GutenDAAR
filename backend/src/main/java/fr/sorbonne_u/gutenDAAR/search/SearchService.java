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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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

    /** The injected regex (on indexed terms) executor. */
    protected final ThreadPoolTaskExecutor regexExecutor;

    /** The injected regex full text executor. */
    protected final ThreadPoolTaskExecutor fullTextExecutor;

    public SearchService(PostingService postingService, BookTextService bookTextService, BookService bookService,
                         @Qualifier(Settings.REGEX_EXECUTOR) ThreadPoolTaskExecutor regexExecutor,
                         @Qualifier(Settings.FULL_TEXT_EXECUTOR) ThreadPoolTaskExecutor fullTextExecutor) {
        this.postingService = postingService;
        this.bookTextService = bookTextService;
        this.bookService = bookService;
        this.regexExecutor = regexExecutor;
        this.fullTextExecutor = fullTextExecutor;
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
        // (delegated to the regexExecutor)
        List<Future<Boolean>> matchedBooks = new ArrayList<>(); // same order as for books
        for (Book b: books) {
            matchedBooks.add(regexExecutor.submit(() ->
                    postingService.getBookTerms(b.getId()).stream()
                            .anyMatch(term -> regex.matcher(term).matches())
            ));
        }
        List<Book> matchingBooks = collectMatchingBooks(books, matchedBooks, "Regex");

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
        // (delegated to the fullTextExecutor)
        List<Future<Boolean>> matchedBooks = new ArrayList<>(); // same order as for books
        for (Book b: books) {
            matchedBooks.add(fullTextExecutor.submit(() -> {
                BookText bookText = bookTextService.findById(b.getId()).get();
                Stream<String> bookLines = bookText.getContent().lines();
                return bookLines.anyMatch(l -> regex.matcher(l).find());
            }));
        }
        List<Book> matchingBooks = collectMatchingBooks(books, matchedBooks, "Regex FULL");

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

    /**
     * Wait for the sub-threads matching results, and keep the matching books.
     *
     * <p>If a task fails or the waiting thread is interrupted, the remaining tasks are canceled.</p>
     *
     * @param books                     The books, in the same order as their matching results.
     * @param matchedBooks              The matching result of each book, given by a sub-thread task.
     * @param searchName                The search name, used in the log and exception messages.
     * @return                          The matching books, in the same order as given.
     * @throws IllegalStateException    If the waiting thread is interrupted, or if a task fails with a checked exception.
     * @throws RuntimeException         The unchecked exception a task failed with.
     */
    protected static List<Book> collectMatchingBooks(List<Book> books, List<Future<Boolean>> matchedBooks,
                                                     String searchName) {
        List<Book> matchingBooks = new ArrayList<>();
        try {
            for (int i = 0; i < books.size(); i++) {
                boolean matchedBook = matchedBooks.get(i).get();
                if (matchedBook) matchingBooks.add(books.get(i));

                int done = i + 1;
                if (done % Settings.PROGRESS_BAR_STEP == 0) {
                    LOG.atInfo()
                            .addArgument(searchName)
                            .addArgument(() -> done)
                            .addArgument(books::size)
                            .log("{} search: {}/{} books scanned.");
                }
            }
        } catch (InterruptedException e) {
            matchedBooks.forEach(f -> f.cancel(true));
            Thread.currentThread().interrupt();
            throw new IllegalStateException(searchName + " search interrupted.", e);
        } catch (ExecutionException e) {
            matchedBooks.forEach(f -> f.cancel(true));
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            } else {
                throw new IllegalStateException(searchName + " search failed.", e.getCause());
            }
        }
        return matchingBooks;
    }
}
