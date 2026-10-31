package fr.sorbonne_u.gutenDAAR.database.repositories;

import fr.sorbonne_u.gutenDAAR.database.entities.JaccardEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

/**
 * Repository corresponding to the {@code jaccard_edges} table.
 */
public interface JaccardEdgeRepository extends JpaRepository<JaccardEdge, JaccardEdge.JaccardEdgeId> {

    /**
     * Find all jaccard edges, for a single direction.
     * @return      All jaccard edges for a single direction.
     */
    @Query("SELECT je FROM JaccardEdge je WHERE je.bookA < je.bookB")
    Set<JaccardEdge> findAllSingleDirection();
}
