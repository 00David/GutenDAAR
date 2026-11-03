package fr.sorbonne_u.gutenDAAR.database.repositories;

import fr.sorbonne_u.gutenDAAR.database.entities.JaccardEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Repository corresponding to the {@code jaccard_edges} table.
 */
public interface JaccardEdgeRepository extends JpaRepository<JaccardEdge, JaccardEdge.JaccardEdgeId> {

    /**
     * Find the edges leaving the given books. Edges are stored in both directions,
     * so this returns every neighbour of these books. Uses the primary key (book_a, book_b).
     * @param bookIds   The books whose edges are wanted.
     * @return          The edges whose book_a is one of the given books.
     */
    List<JaccardEdge> findByBookAIn(Collection<Integer> bookIds);

    /**
     * Find all jaccard edges, for a single direction.
     * @return      All jaccard edges for a single direction.
     */
    @Query("SELECT je FROM JaccardEdge je WHERE je.bookA < je.bookB")
    Set<JaccardEdge> findAllSingleDirection();
}
