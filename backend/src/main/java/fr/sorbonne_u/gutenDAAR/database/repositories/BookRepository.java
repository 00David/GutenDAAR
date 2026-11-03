package fr.sorbonne_u.gutenDAAR.database.repositories;

import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository corresponding to the {@code books} table.
 */
public interface BookRepository extends JpaRepository<Book, Integer> {

    /**
     * Indicate if at least one book has no computed closeness centrality yet.
     * @return      true if a book has a {@code null} centrality.
     */
    boolean existsByClosenessCentralityIsNull();

    /**
     * Indicate if at least one book has no computed betweenness centrality yet.
     * @return      true if a book has a {@code null} centrality.
     */
    boolean existsByBetweennessCentralityIsNull();

    /**
     * Indicate if at least one book has no computed pagerank centrality yet.
     * @return      true if a book has a {@code null} centrality.
     */
    boolean existsByPagerankCentralityIsNull();

    /**
     * Indicate if at least one book has no computed centrality yet.
     * @return      true if a book has a {@code null} centrality.
     */
    default boolean existsByCentralityIsNull() {
        // TODO: also consider closeness and betweenness when implemented
        return existsByPagerankCentralityIsNull(); // || existsByClosenessCentralityIsNull() || existsByBetweennessCentralityIsNull()
    }
}
