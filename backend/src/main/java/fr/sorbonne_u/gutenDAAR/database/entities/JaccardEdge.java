package fr.sorbonne_u.gutenDAAR.database.entities;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "jaccard_edges")
@IdClass(JaccardEdge.JaccardEdgeId.class)
/**
 * Entity corresponding to a link between two books in the jaccard graph.
 *
 * <p>It can be used both for forward indexing and for inverted indexing.</p>
 *
 * <p>The entity is persistable: on a first save, a useless SELECT method is avoided.</p>
 */
public class JaccardEdge implements Persistable<JaccardEdge.JaccardEdgeId> {
    /** Not a column: true until the edge has been saved or loaded from the database. */
    @Transient
    private boolean isNew = true;

    /** The identifier of a jaccard edge: both books linked by the edge. */
    public record JaccardEdgeId(int bookA, int bookB) {}
    /** The unique Gutenberg id of the book A. */
    @Id
    @Column(name = "book_a")
    protected int bookA;
    /** The unique Gutenberg id of the book B. */
    @Id
    @Column(name = "book_b")
    protected int bookB;
    /** The computed jaccard distance within the graph. */
    protected double distance;

    protected JaccardEdge() {}

    public JaccardEdge(int bookA, int bookB, double distance) {
        this.bookA = bookA;
        this.bookB = bookB;
        this.distance = distance;
    }

    @Override
    public JaccardEdgeId getId() {
        return new JaccardEdgeId(bookA, bookB);
    }

    public int getBookA() {
        return bookA;
    }

    public int getBookB() {
        return bookB;
    }

    public double getDistance() {
        return distance;
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
