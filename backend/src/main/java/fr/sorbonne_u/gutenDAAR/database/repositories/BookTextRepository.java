package fr.sorbonne_u.gutenDAAR.database.repositories;

import fr.sorbonne_u.gutenDAAR.database.entities.BookText;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository corresponding to the {@code book_texts} table.
 */
public interface BookTextRepository extends JpaRepository<BookText, Integer> {}
