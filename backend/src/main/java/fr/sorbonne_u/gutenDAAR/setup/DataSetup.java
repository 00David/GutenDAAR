package fr.sorbonne_u.gutenDAAR.setup;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.Utils;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.JaccardEdge;
import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import fr.sorbonne_u.gutenDAAR.database.services.BookService;
import fr.sorbonne_u.gutenDAAR.database.services.JaccardEdgeService;
import fr.sorbonne_u.gutenDAAR.database.services.PostingService;
import fr.sorbonne_u.gutenDAAR.setup.centrality.BookIdInMatrix;
import fr.sorbonne_u.gutenDAAR.setup.centrality.CentralityI;
import fr.sorbonne_u.gutenDAAR.setup.centrality.PageRank;
import fr.sorbonne_u.gutenDAAR.setup.retrieval.GutenDAARCatalogCSV;
import fr.sorbonne_u.gutenDAAR.setup.retrieval.GutenDAARCatalogCSV.GutenDAAREntry;

import fr.sorbonne_u.gutenDAAR.setup.retrieval.Covers;
import fr.sorbonne_u.gutenDAAR.setup.retrieval.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Fill the database at application startup, from the GutenDAAR catalog and the Gutenberg mirror.
 */
@Component
public class DataSetup implements ApplicationRunner {

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(DataSetup.class);

    /** The injected book service. */
    protected final BookService bookService;
    /** The injected jaccard edge service. */
    protected final JaccardEdgeService jeService;
    /** The injected posting service. */
    protected final PostingService postingService;

    public DataSetup(BookService bookService, JaccardEdgeService jeService, PostingService postingService) {
        this.bookService = bookService;
        this.jeService = jeService;
        this.postingService = postingService;
    }

    /**
     * Run the whole setup if the database is empty, and do nothing if it has already been completed.
     * @param args                      The application arguments, unused.
     * @throws IllegalStateException    If the database only contains a partial setup.
     * @throws Exception                If an error occurs during the setup.
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (bookService.isSetupDone()) {
            LOG.info("Database already set up: skipping the setup.");
            return;
        }
        if (!bookService.isEmpty()) {
            throw new IllegalStateException("Incomplete database from a previous setup: restart with the 'reset' profile.");
        }

        List<GutenDAAREntry> catalog = GutenDAARCatalogCSV.retrieve();
        LOG.info("1/5 - Catalog ready: {} book references.", catalog.size());

        initBooksWithoutCentrality(catalog);
        LOG.info("2/5 - books table partially ready (without centrality computed).");

        initPostings(catalog);
        LOG.info("3/5 - postings table ready.");

        initJaccardEdges(catalog);
        LOG.info("4/5 - jaccard_edges table ready.");

        updateBooksCentrality(catalog);
        LOG.info("5/5 - books table fully ready (with centrality computed).");

        LOG.info("SETUP COMPLETED");
    }

    /**
     * Retrieve the small covers, and insert the books without their centrality.
     * A book without retrievable cover is still inserted.
     * @param catalog                   The entries of the books to insert.
     * @throws InterruptedException     If an interruption occurs while retrieving a cover.
     */
    protected void initBooksWithoutCentrality(List<GutenDAAREntry> catalog) throws InterruptedException {
        List<Book> books = new ArrayList<>();

        // For each book
        for (int i = 0; i < catalog.size(); i++) {
            GutenDAAREntry book = catalog.get(i);
            books.add(new Book(book.id(), book.title(), book.authors(), book.language(), book.wordCount()));

            try {
                // Retrieve its small cover if not already present (and save it on the disk)
                Covers.retrieveSmallCover(book.id());
            } catch (IOException e) {
                LOG.warn("No cover for book {}: {}", book.id(), e.getMessage());
            }

            if (books.size() % Settings.PROGRESS_BAR_STEP == 0) {
                LOG.atInfo().addArgument(() -> Utils.progressBar(books.size(), catalog.size())).log("Retrieving small book covers {}");
            }
        }
        bookService.insert(books);
    }

    /**
     * Insert the postings of each book, then create the inverted index.
     * A book whose text cannot be retrieved stays without postings.
     * @param catalog                   The entries of the books to index, already inserted.
     * @throws InterruptedException     If an interruption occurs while retrieving a book text.
     */
    protected void initPostings(List<GutenDAAREntry> catalog) throws InterruptedException{
        // For each book
        for (int i = 0; i < catalog.size(); i++) {
            GutenDAAREntry book = catalog.get(i);

            try {
                // Retrieve the book words
                List<String> words = Library.retrieveTreatedText(book.id());

                // Build the map associating to each book word -> its n° of occurrences
                Map<String, Integer> occurrences = new HashMap<>();
                for (String word: words) {
                    occurrences.merge(word, 1, Integer::sum);
                }

                // Insert into the postings table the book's map associations
                List<Posting> postings = new ArrayList<>();
                for (Map.Entry<String, Integer> occurrence: occurrences.entrySet()) {
                    String word = occurrence.getKey();
                    int nbOccurrences = occurrence.getValue();
                    postings.add(new Posting(book.id(), word, nbOccurrences));
                }
                postingService.insert(postings);
            } catch (IOException e) {
                LOG.warn("Skipped book {} postings: {}", book.id(), e.getMessage());
            }

            int done = i + 1;
            if (done % Settings.PROGRESS_BAR_STEP == 0) {
                LOG.atInfo().addArgument(() -> Utils.progressBar(done, catalog.size())).log("Building book postings {}");
            }
        }

        // Create postings inverted index
        postingService.createInvertedIndex();
    }

    /**
     * Insert the jaccard edges, computed for each book pair from the postings.
     * Two books without any term are not linked.
     * @param catalog       The entries of the books to link, with their postings already inserted.
     */
    protected void initJaccardEdges(List<GutenDAAREntry> catalog) {
        List<JaccardEdge> edges = new ArrayList<>();

        // Load the terms only once per book
        List<Set<String>> terms = new ArrayList<>();
        for (GutenDAAREntry book: catalog) {
            terms.add(postingService.getBookTerms(book.id()));
        }

        // For each book pair, in a single direction, compute the jaccard distance between them
        for (int aI = 0; aI < catalog.size(); aI++) {
            for (int bI = aI + 1; bI < catalog.size(); bI++) {
                GutenDAAREntry bookA = catalog.get(aI);
                GutenDAAREntry bookB = catalog.get(bI);

                Set<String> bookATerms = terms.get(aI);
                Set<String> bookBTerms = terms.get(bI);

                Set<String> inter = new HashSet<>(bookATerms);
                inter.retainAll(bookBTerms);

                int unionSize = bookATerms.size() + bookBTerms.size() - inter.size();
                if (unionSize == 0) {
                    continue; // two books without any term
                }

                double jaccardDistance = 1.0 - ((double) inter.size() / unionSize);
                edges.add(new JaccardEdge(bookA.id(), bookB.id(), jaccardDistance));
            }
        }

        // Insert all edges
        // (the method will in fact insert each edge in both directions, so 2 insertions per edge  in the list)
        jeService.insert(edges);
    }

    /**
     * Compute the book centralities on the jaccard graph, and save them.
     * @param catalog       The entries of the books to update, with their jaccard edges already inserted.
     */
    protected void updateBooksCentrality(List<GutenDAAREntry> catalog) {
        // The adjacency matrix is built, before calling the centrality computation with it
        BookIdInMatrix bookIdInMatrix = jeService.buildAdjacencyMatrix();
        CentralityI pr = new PageRank();
        double[] ranks = pr.compute(bookIdInMatrix.matrix());

        // For each book, get its rank in the computed vector and update its centrality in DB
        for (GutenDAAREntry book: catalog) {
            int id = book.id();
            int indexInMatrix = bookIdInMatrix.bookIdToMatrixIndex().get(id);
            double centrality = ranks[indexInMatrix];
            bookService.updateCentrality(id, centrality);
        }
    }
}
