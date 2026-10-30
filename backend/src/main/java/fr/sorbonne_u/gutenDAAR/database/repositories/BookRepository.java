package fr.sorbonne_u.gutenDAAR.database.repositories;

import fr.sorbonne_u.gutenDAAR.database.entities.Book;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository corresponding to the {@code books} table.
 */
public interface BookRepository extends JpaRepository<Book, Integer> {

    /**
     * Indicate if at least one book has no computed centrality yet.
     * @return      true if a book has a {@code null} centrality.
     */
    boolean existsByCentralityIsNull();
}
