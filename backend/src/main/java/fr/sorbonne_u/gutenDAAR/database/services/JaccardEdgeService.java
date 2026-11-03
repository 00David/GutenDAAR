package fr.sorbonne_u.gutenDAAR.database.services;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import fr.sorbonne_u.gutenDAAR.database.entities.JaccardEdge;
import fr.sorbonne_u.gutenDAAR.database.repositories.BookRepository;
import fr.sorbonne_u.gutenDAAR.database.repositories.JaccardEdgeRepository;
import fr.sorbonne_u.gutenDAAR.setup.centrality.BookIdInMatrix;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Service allowing to interact with {@link JaccardEdgeRepository}.
 */
@Service
public class JaccardEdgeService {
    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(JaccardEdgeService.class);

    /** The injected jaccard edge repository. */
    protected final JaccardEdgeRepository jeRepository;
    /** The injected book repository. */
    protected final BookRepository bookRepository;
    /** The injected entity manager. */
    protected final EntityManager em;

    public JaccardEdgeService(JaccardEdgeRepository jeRepository, BookRepository bookRepository, EntityManager em) {
        this.jeRepository = jeRepository;
        this.bookRepository = bookRepository;
        this.em = em;
    }

    /**
     * Insert the given edges, for both directions, in a single transaction: if one insertion fails, none is kept.
     *
     * <p>Edges must be given in a <strong>single direction</strong>, not in both, and must not link the same book.</p>
     *
     * <p>
     * The edges are sent to the database in JDBC batches of {@link Settings#BATCH_SIZE} rows,
     * and the persistence context is cleared after each batch, so that it does not keep
     * every inserted edge in memory.
     * </p>
     *
     * @param edges     The edges to insert, none of which may already exist in the database, in a single direction.
     */
    @Transactional
    public void insert(List<JaccardEdge> edges) {
        for (int i = 0; i < edges.size(); i++) {
            JaccardEdge edge = edges.get(i);
            jeRepository.save(edge);
            jeRepository.save(new JaccardEdge(edge.getBookB(), edge.getBookA(), edge.getDistance()));
            if (((i + 1) * 2) % Settings.BATCH_SIZE == 0) { // *2 because 2 saves per iteration
                em.flush();
                em.clear();
                LOG.debug("Saved jaccard edges and cleared persistence context after {} iterations.", i);
            }
        }
        LOG.info("Inserted {} jaccard edges.", edges.size());
    }

    /**
     * Find the edges leaving the given books, i.e. all their neighbours in the jaccard graph
     * (edges are stored in both directions).
     * @param bookIds   The books whose neighbours are wanted.
     * @return          The edges whose first book is one of the given books.
     */
    public List<JaccardEdge> findEdgesFrom(Collection<Integer> bookIds) {
        return jeRepository.findByBookAIn(bookIds);
    }

    /**
     * Build the adjacency matrix of the jaccard graph, using the books and jaccard edges currently present in DB.
     *
     * <p>
     * The matrix is symmetric. The weight of a link is the jaccard similarity of its books (1 - distance),
     * so that the most similar books are the most strongly linked.
     * </p>
     *
     * <p>Each book has its index in the matrix: the row of a book without any edge is empty.</p>
     *
     * @return      The adjacency matrix of the jaccard graph, with the matrix index of each book id.
     */
    public BookIdInMatrix buildAdjacencyMatrix() {
        List<Book> books = bookRepository.findAll();

        Map<Integer, Integer> bookIdToMatrixIndex = new HashMap<>();
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            bookIdToMatrixIndex.put(book.getId(), i);
        }

        double[][] adjacencyMatrix = new double[books.size()][books.size()];

        Set<JaccardEdge> edges = jeRepository.findAllSingleDirection();
        for (JaccardEdge edge: edges) {
            int bookAId = edge.getBookA();
            int bookBId = edge.getBookB();
            double weight = 1 - edge.getDistance();

            Integer aMatrixIndex = bookIdToMatrixIndex.get(bookAId);
            Integer bMatrixIndex = bookIdToMatrixIndex.get(bookBId);

            adjacencyMatrix[aMatrixIndex][bMatrixIndex] = weight;
            adjacencyMatrix[bMatrixIndex][aMatrixIndex] = weight;
        }

        LOG.info("Adjacency matrix built for {} books.", bookIdToMatrixIndex.size());
        return new BookIdInMatrix(bookIdToMatrixIndex, adjacencyMatrix);
    }
}
