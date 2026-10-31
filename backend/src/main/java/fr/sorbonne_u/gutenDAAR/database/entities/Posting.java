package fr.sorbonne_u.gutenDAAR.database.entities;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

/**
 * Entity corresponding to a link between a book and one of its terms,
 * in the graph used both for forward indexing and for inverted indexing.
 *
 * <p>The entity is persistable: on a first save, a useless SELECT method is avoided.</p>
 */
@Entity
@Table(name = "postings")
@IdClass(Posting.PostingId.class)
public class Posting implements Persistable<Posting.PostingId> {
    /** Not a column: true until the posting has been saved or loaded from the database. */
    @Transient
    private boolean isNew = true;

    /** The identifier of a posting: a book and one of its terms. */
    public record PostingId(int bookId, String term) {}

    /** The unique Gutenberg id. */
    @Id
    protected int bookId;
    /** The term contained by the book {@link #bookId}. */
    @Id
    protected String term;
    /** The number of {@link #term} occurrences within {@link #bookId}. Must be strictly positive. */
    protected int occurrences;

    protected Posting() {}

    public Posting(int bookId, String term, int occurrences) {
        this.bookId = bookId;
        this.term = term;
        this.occurrences = occurrences;
    }

    @Override
    public PostingId getId() {
        return new PostingId(bookId, term);
    }

    public int getBookId() {
        return bookId;
    }

    public String getTerm() {
        return term;
    }

    public int getOccurrences() {
        return occurrences;
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
