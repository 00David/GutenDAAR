package fr.sorbonne_u.gutenDAAR.setup;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.Utils;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.BookText;
import fr.sorbonne_u.gutenDAAR.database.entities.JaccardEdge;
import fr.sorbonne_u.gutenDAAR.database.entities.Posting;
import fr.sorbonne_u.gutenDAAR.database.services.BookService;
import fr.sorbonne_u.gutenDAAR.database.services.BookTextService;
import fr.sorbonne_u.gutenDAAR.database.services.JaccardEdgeService;
import fr.sorbonne_u.gutenDAAR.database.services.PostingService;
import fr.sorbonne_u.gutenDAAR.search.Vocabulary;
import fr.sorbonne_u.gutenDAAR.setup.centrality.BookIdInMatrix;
import fr.sorbonne_u.gutenDAAR.setup.centrality.CentralityI;
import fr.sorbonne_u.gutenDAAR.setup.centrality.PageRank;
import fr.sorbonne_u.gutenDAAR.setup.retrieval.GutenDAARCatalogCSV;
import fr.sorbonne_u.gutenDAAR.setup.retrieval.GutenDAARCatalogCSV.GutenDAAREntry;

import fr.sorbonne_u.gutenDAAR.setup.retrieval.covers.CoverType;
import fr.sorbonne_u.gutenDAAR.setup.retrieval.covers.Covers;
import fr.sorbonne_u.gutenDAAR.setup.retrieval.Library;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Fill the database at application startup, from the GutenDAAR catalog and the Gutenberg mirror.
 */
@Component
public class DataSetup implements ApplicationRunner {

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(DataSetup.class);

    /** The injected book service. */
    protected final BookService bookService;
    /** The injected book text service. */
    protected final BookTextService bookTextService;
    /** The injected jaccard edge service. */
    protected final JaccardEdgeService jeService;
    /** The injected posting service. */
    protected final PostingService postingService;

    /** The injected vocabulary, kept in memory. */
    protected final Vocabulary vocabulary;

    public DataSetup(BookService bookService, BookTextService bookTextService,
                     JaccardEdgeService jeService, PostingService postingService,
                     Vocabulary vocabulary) {
        this.bookService = bookService;
        this.bookTextService = bookTextService;
        this.jeService = jeService;
        this.postingService = postingService;
        this.vocabulary = vocabulary;
    }

    /**
     * Run the whole setup if the database is empty, and do nothing if it has already been completed.
     * @param args                      The application arguments, unused.
     * @throws IllegalStateException    If the database only contains a partial setup.
     * @throws Exception                If an error occurs during the setup.
     */
    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {
        if (bookService.isSetupDone()) {
            LOG.info("Database already set up: skipping the setup.");
            vocabulary.load();
            return;
        }
        if (!bookService.isEmpty()) {
            throw new IllegalStateException("Incomplete database from a previous setup: restart with the 'reset' profile.");
        }

        List<GutenDAAREntry> catalog = GutenDAARCatalogCSV.retrieve();
        LOG.info("1/6 - Catalog ready: {} book references.", catalog.size());

        initBooksWithoutCentrality(catalog, Settings.COVER_TYPE);
        LOG.info("2/6 - books table partially ready (without centrality computed).");

        initBookTextsAndPostings(catalog);
        LOG.info("3/6 - book texts and postings tables ready.");

        initJaccardEdges(catalog);
        LOG.info("4/6 - jaccard_edges table ready.");

        updateBooksCentrality(catalog);
        LOG.info("5/6 - books table fully ready (with centrality computed).");

        vocabulary.load();
        LOG.info("6/6 - vocabulary (distinct indexed terms) loaded in memory.");

        LOG.info("SETUP COMPLETED");
    }

    /**
     * Retrieve the small covers, and insert the books without their centrality.
     * A book without retrievable cover is still inserted.
     * @param catalog                   The entries of the books to insert.
     * @param coverType                 The book cover type to retrieve for each book.
     * @throws InterruptedException     If an interruption occurs while retrieving a cover.
     */
    protected void initBooksWithoutCentrality(List<GutenDAAREntry> catalog, CoverType coverType) throws InterruptedException {
        List<Book> books = new ArrayList<>();

        // For each book
        for (int i = 0; i < catalog.size(); i++) {
            GutenDAAREntry book = catalog.get(i);
            books.add(new Book(book.id(), book.title(), book.authors(), book.language(), book.wordCount()));

            try {
                // Retrieve its original cover if not already present (and save it on the disk)
                Covers.retrieve(book.id(), coverType);
            } catch (IOException e) {
                LOG.warn("No cover for book {}: {}", book.id(), e.getMessage());
            }

            if (books.size() % Settings.PROGRESS_BAR_STEP == 0) {
                LOG.atInfo()
                        .addArgument(() -> coverType)
                        .addArgument(() -> Utils.progressBar(books.size(), catalog.size()))
                        .log("Retrieving {} book covers {}");
            }
        }
        bookService.insert(books);
    }

    /**
     * Insert the raw text and postings of each book, then create the inverted index.
     * A book whose text cannot be retrieved stays without postings.
     * @param catalog                   The entries of the books to index, already inserted.
     * @throws InterruptedException     If an interruption occurs while retrieving a book text.
     */
    protected void initBookTextsAndPostings(List<GutenDAAREntry> catalog) throws InterruptedException{
        LOG.atInfo()
                .addArgument(() -> Utils.progressBar(0, catalog.size()))
                .log("Building book postings {}");
        // For each book
        for (int i = 0; i < catalog.size(); i++) {
            GutenDAAREntry book = catalog.get(i);

            try {
                // Retrieve the book text
                Library.RetrievedText text  = Library.retrieveText(book.id(), book.language());

                // Insert into the book texts table the raw text
                bookTextService.insert(new BookText(book.id(), text.rawText()));

                // Build the map associating each book indexed term -> its n° of occurrences
                Map<String, Integer> occurrences = new HashMap<>();
                for (String term: text.terms()) {
                    occurrences.merge(term, 1, Integer::sum);
                }

                // Insert into the postings table the book's map associations
                List<Posting> postings = new ArrayList<>();
                for (Map.Entry<String, Integer> occurrence: occurrences.entrySet()) {
                    String term = occurrence.getKey();
                    int nbOccurrences = occurrence.getValue();
                    postings.add(new Posting(book.id(), term, nbOccurrences));
                }
                postingService.insert(postings);
            } catch (IOException e) {
                LOG.warn("Skipped book {} postings: {}", book.id(), e.getMessage());
            }

            int done = i + 1;
            if (done % Settings.PROGRESS_BAR_STEP == 0) {
                LOG.atInfo()
                        .addArgument(() -> Utils.progressBar(done, catalog.size()))
                        .log("Building book postings {}");
            }
        }

        // Create postings inverted index
        postingService.createInvertedIndex();
    }

    /**
     * Insert the jaccard edges, computed for each book pair from the postings,
     * keeping only the edges to the {@link Settings#JACCARD_NEIGHBOURS} nearest neighbours of each book.
     * Two books without any term are not linked.
     * @param catalog       The entries of the books to link, with their postings already inserted.
     */
    protected void initJaccardEdges(List<GutenDAAREntry> catalog) {
        // Load the words only once per book
        List<Set<String>> terms = catalog.stream()
                .map(e -> postingService.getBookTerms(e.id()))
                .toList();

        // For each book pair, in a single direction, compute the jaccard distance between them.
        // The rows (book A) are computed in parallel, on the common fork-join pool:
        // the terms are only read, and each row returns its own edges.
        AtomicInteger rowsDone = new AtomicInteger();
        List<JaccardEdge> edges = IntStream.range(0, catalog.size())
                .parallel()
                .mapToObj(aI -> {
                    List<JaccardEdge> rowEdges = new ArrayList<>();
                    for (int bI = aI + 1; bI < catalog.size(); bI++) {
                        GutenDAAREntry bookA = catalog.get(aI);
                        GutenDAAREntry bookB = catalog.get(bI);

                        Set<String> bookATerms = terms.get(aI);
                        Set<String> bookBTerms = terms.get(bI);

                        Set<String> biggerSet;
                        Set<String> smallerSet;
                        if (bookATerms.size() < bookBTerms.size()) {
                            biggerSet = bookBTerms;
                            smallerSet = bookATerms;
                        } else {
                            biggerSet = bookATerms;
                            smallerSet = bookBTerms;
                        }
                        int interSize = 0;
                        for (String term: smallerSet) {
                            if (biggerSet.contains(term)) {
                                interSize++;
                            }
                        }

                        int unionSize = bookATerms.size() + bookBTerms.size() - interSize;
                        if (unionSize == 0) {
                            continue; // two books without any term
                        }

                        double jaccardDistance = 1.0 - ((double) interSize / unionSize);
                        rowEdges.add(new JaccardEdge(bookA.id(), bookB.id(), jaccardDistance));
                    }

                    // Rows end in any order: count the completed ones
                    int done = rowsDone.incrementAndGet();
                    if (done % Settings.PROGRESS_BAR_STEP == 0) {
                        LOG.atInfo()
                                .addArgument(() -> Utils.progressBar(done, catalog.size()))
                                .log("Building Jaccard graph {}");
                    }
                    return rowEdges;
                })
                .flatMap(List::stream)
                .toList(); // in the rows order

        LOG.atInfo()
            .addArgument(() -> Utils.progressBar(rowsDone.get(), catalog.size()))
            .log("Building Jaccard graph {}");

        // Only keep the edges to the nearest neighbours of each book
        List<JaccardEdge> keptEdges = keepNearestNeighbours(edges, Settings.JACCARD_NEIGHBOURS);
        LOG.info("Kept {} jaccard edges out of {} ({} nearest neighbours per book).",
                keptEdges.size(), edges.size(), Settings.JACCARD_NEIGHBOURS);

        // Insert the kept edges
        // (the method will in fact insert each edge in both directions, so 2 insertions per edge in the list)
        jeService.insert(keptEdges);
    }

    /**
     * Keep, for each book, only its edges to its k nearest neighbours (smallest jaccard distances).
     * An edge is kept if one of its two books is among the k nearest neighbours of the other: no book is then isolated.
     * @param edges     The edges, in a single direction.
     * @param k         The number of nearest neighbours to keep for each book.
     * @return          The kept edges, still in a single direction, without duplicates.
     */
    protected static List<JaccardEdge> keepNearestNeighbours(List<JaccardEdge> edges, int k) {
        // Edges of each book (each edge belongs to its 2 books)
        Map<Integer, List<JaccardEdge>> bookEdges = new HashMap<>();
        for (JaccardEdge edge : edges) {
            bookEdges.computeIfAbsent(edge.getBookA(), id -> new ArrayList<>()).add(edge);
            bookEdges.computeIfAbsent(edge.getBookB(), id -> new ArrayList<>()).add(edge);
        }

        // Keep the k nearest edges of each book; an edge kept by its 2 books is only kept once
        // (identity set: JaccardEdge does not override equals, and the same object is shared by both books)
        Set<JaccardEdge> kept = Collections.newSetFromMap(new IdentityHashMap<>());
        for (List<JaccardEdge> edgesOfBook : bookEdges.values()) {
            edgesOfBook.sort(Comparator.comparingDouble(JaccardEdge::getDistance));
            kept.addAll(edgesOfBook.subList(0, Math.min(k, edgesOfBook.size())));
        }
        return new ArrayList<>(kept);
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
            double pagerankCentrality = ranks[indexInMatrix];
            // TODO: pass computed closeness and betweenness centralities
            bookService.updateCentrality(id, null, null, pagerankCentrality);
        }
    }
}
