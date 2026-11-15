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
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /** The injected vocabulary. */
    protected final Vocabulary vocabulary;

    /** The injected regex (on indexed terms) executor. */
    protected final ThreadPoolTaskExecutor regexExecutor;

    /** The injected regex full text executor. */
    protected final ThreadPoolTaskExecutor fullTextExecutor;

    public SearchService(PostingService postingService, BookTextService bookTextService,
                         BookService bookService, Vocabulary vocabulary,
                         @Qualifier(Settings.REGEX_EXECUTOR) ThreadPoolTaskExecutor regexExecutor,
                         @Qualifier(Settings.FULL_TEXT_EXECUTOR) ThreadPoolTaskExecutor fullTextExecutor) {
        this.postingService = postingService;
        this.bookTextService = bookTextService;
        this.bookService = bookService;
        this.vocabulary = vocabulary;
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
     * Search the books containing an indexed term matching the pattern,
     * and rank them according to the given ranking method.
     *
     * @param regex                     The regex pattern.
     * @param rankingMethod             The ranking method to apply.
     * @return                          Books (using their indexed terms) matching the regex, ranked by the ranking method.
     * @throws IllegalStateException    If the vocabulary is not loaded yet.
     */
    public List<BookSearchedDTO> searchByRegex(Pattern regex, RankingMethod rankingMethod) {
        // A semaphore is used for submitting a limited amount of simultaneous tasks for
        // this request: this ensures not overloading the executor with only this request's tasks
        Semaphore limitSimultaneousTasks = new Semaphore(Settings.REGEX_THREADS);

        // First get the distinct terms matching the regex, by applying it on the vocabulary
        // (delegated to the regexExecutor: each task scans a slice of the vocabulary)
        List<String> terms = vocabulary.getDistinctTerms();
        int termsPerTask = searchByRegexTermsPerTask(terms.size());

        List<CompletableFuture<List<String>>> matchingTermSlices = new ArrayList<>(); // same order as for terms
        for (int i = 0; i < terms.size(); i += termsPerTask) {
            try {
                limitSimultaneousTasks.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Regex search interrupted.", e);
            }

            int iFirstTerm = i; // included
            int iLastTerm = Math.min(i + termsPerTask, terms.size()); // excluded
            matchingTermSlices.add(regexExecutor.submitCompletable(() -> {
                try {
                    List<String> slice = terms.subList(iFirstTerm, iLastTerm);
                    return slice.stream()
                            .filter(term -> regex.matcher(term).matches())
                            .toList();
                } finally {
                    limitSimultaneousTasks.release();
                }
            }));
        }

        List<String> matchingTerms = matchingTermSlices.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
        LOG.atDebug()
                .addArgument(matchingTerms::size)
                .addArgument(terms::size)
                .log("Regex search: {}/{} distinct terms matching.");

        // Then get the books containing at least one matching term (inverted index lookup)
        Map<Integer, Book> books = bookService.findAllById(postingService.findBookIdsContainingAny(matchingTerms));

        // Sort the books according to the ranking method
        List<Book> matchingBooks = RankingMethod.sortByCentrality(books.values(), rankingMethod);

        // Finally, return the books as DTOs (with their occurrences not provided)
        List<BookSearchedDTO> results = BookSearchedDTO.from(matchingBooks);
        LOG.atInfo()
                .addArgument(results::size)
                .addArgument(regex::toString)
                .log("Found {} books after '{}' regex search.");
        return results;
    }

    /**
     * Compute the number of vocabulary terms scanned per task, for the regex search.
     * @param terms     Total number of terms in the vocabulary.
     * @return          The number of terms scanned per task, at least 1.
     */
    protected static int searchByRegexTermsPerTask(int terms) {
        return Math.max(1, Math.ceilDiv(terms, Settings.REGEX_THREADS * Settings.TASKS_PER_THREAD));
    }

    /**
     * Search the books containing a word matching the pattern, and rank them according to the given ranking method.
     *
     * <p>
     * A timeout fixed by {@link Settings#FULL_TEXT_SEARCH_TIMEOUT_SECONDS} can interrupt
     * the search.
     * </p>
     *
     * @param regex             The regex pattern.
     * @param rankingMethod     The ranking method to apply.
     * @return                  Books (using their words) matching the regex, ranked by the ranking method.
     */
    public List<BookSearchedDTO> searchByRegexFullText(Pattern regex, RankingMethod rankingMethod) {
        // A semaphore is used for submitting a limited amount of simultaneous tasks for
        // this request: this ensures not overloading the executor with only this request's tasks
        Semaphore limitSimultaneousTasks = new Semaphore(Settings.FULL_TEXT_THREADS);

        // First get all books
        List<Book> books = bookService.findAll();

        // Then, filter them by keeping those with word(s) matching the given regex
        // (delegated to the fullTextExecutor)
        AtomicBoolean cancelled = new AtomicBoolean();
        long deadline = searchByRegexFullTextDeadline();
        List<Future<Boolean>> matchedBooks = new ArrayList<>(); // same order as for books
        for (Book b: books) {
            try {
                limitSimultaneousTasks.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Regex FULL search interrupted.", e);
            }

            matchedBooks.add(fullTextExecutor.submit(() -> {
                try {
                    if (cancelled.get()) return false;
                    BookText bookText = bookTextService.findById(b.getId()).get();
                    Stream<String> bookLines = bookText.getContent().lines();
                    return bookLines
                            .takeWhile(l -> !cancelled.get()) // stops between two lines once canceled
                            .anyMatch(l -> regex.matcher(l).find());
                } finally {
                    limitSimultaneousTasks.release();
                }
            }));
        }

        List<Book> matchingBooks = new ArrayList<>();
        try {
            for (int i = 0; i < books.size(); i++) {
                long timeout = deadline - System.nanoTime();
                boolean matchedBook = matchedBooks.get(i).get(timeout, TimeUnit.NANOSECONDS);
                if (matchedBook) {
                    matchingBooks.add(books.get(i));
                }

                int done = i + 1;
                if (done % Settings.PROGRESS_BAR_STEP == 0) {
                    LOG.atInfo()
                            .addArgument(() -> done)
                            .addArgument(books::size)
                            .log("Regex FULL search: {}/{} books scanned.");
                }
            }
        } catch (TimeoutException e) {
            cancel(cancelled, matchedBooks);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Regex full-text search took too long.");
        } catch (InterruptedException e) {
            cancel(cancelled, matchedBooks);
            matchedBooks.forEach(f -> f.cancel(true));
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Regex FULL search interrupted.", e);
        } catch (ExecutionException e) {
            cancel(cancelled, matchedBooks);
            matchedBooks.forEach(f -> f.cancel(true));
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            } else {
                throw new IllegalStateException("Regex FULL search failed.", e.getCause());
            }
        }

        // Sort the books according to the ranking method
        RankingMethod.sortInPlaceByCentrality(matchingBooks, rankingMethod);

        // Finally, return the books as DTOs (with their occurrences not provided)
        List<BookSearchedDTO> results = BookSearchedDTO.from(matchingBooks);
        LOG.atInfo()
                .addArgument(results::size)
                .addArgument(regex::toString)
                .log("Found {} books after '{}' regex FULL search.");
        return results;
    }

    /**
     * Compute the deadline of a regex full text search started now,
     * {@link Settings#FULL_TEXT_SEARCH_TIMEOUT_SECONDS} later.
     * @return          The deadline, as a {@link System#nanoTime()} value.
     */
    protected static long searchByRegexFullTextDeadline() {
        return System.nanoTime() + TimeUnit.SECONDS.toNanos(Settings.FULL_TEXT_SEARCH_TIMEOUT_SECONDS);
    }

    /**
     * Cancel a search: stop its running tasks as soon as possible, and drop its queued ones.
     * @param cancelled     Indicates if the given tasks are indicated as canceled.
     * @param tasks         The tasks to cancel.
     */
    protected void cancel(AtomicBoolean cancelled, List<? extends Future<?>> tasks) {
        // checked by the tasks
        cancelled.set(true);

        // marks queued tasks as canceled, interrupts running ones
        tasks.forEach(f -> f.cancel(true));

        // removes the cancelled ones from the queue right away
        fullTextExecutor.getThreadPoolExecutor().purge();
    }
}
