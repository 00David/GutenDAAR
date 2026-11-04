package fr.sorbonne_u.gutenDAAR.database.entities;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

/**
 * Entity corresponding to a book text into the database.
 *
 * <p>The entity is persistable: on a first save, a useless SELECT method is avoided.</p>
 */
@Entity
@Table(name = "book_texts")
public class BookText implements Persistable<Integer> {
    /** Not a column: true until the book text has been saved or loaded from the database. */
    @Transient
    private boolean isNew = true;

    /** The unique Gutenberg id. */
    @Id
    @Column(name = "book_id")
    protected int bookId;
    /** The book original content (including Gutenberg boilerplate). */
    protected String content;

    protected BookText() {}

    public BookText(int bookId, String content) {
        this.bookId = bookId;
        this.content = content;
    }

    /**
     * Returns the identifier of this book text.
     * <p>
     * The return type is the {@link Integer} wrapper class so that it can be used
     * by classes as a generic type argument.
     * </p>
     * @return the unique Gutenberg book id, never {@code null}
     */
    @Override
    public Integer getId() {
        return bookId;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        isNew = false;
    }
}
