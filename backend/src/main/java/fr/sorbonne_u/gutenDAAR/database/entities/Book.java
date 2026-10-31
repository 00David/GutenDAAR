package fr.sorbonne_u.gutenDAAR.database.entities;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

/**
 * Entity corresponding to a book into the database.
 *
 * <p>It notably maintains its computed centrality in the jaccard graph.</p>
 *
 * <p>The entity is persistable: on a first save, a useless SELECT method is avoided.</p>
 */
@Entity
@Table(name = "books")
public class Book implements Persistable<Integer> {
    /** Not a column: true until the book has been saved or loaded from the database. */
    @Transient
    private boolean isNew = true;

    /** The unique Gutenberg id. */
    @Id
    protected int id;
    /** The book title. */
    protected String title;
    /** The book authors. Can be {@code null} if there are no authors. */
    protected String author;
    /** The book language, as an ISO code, for instance {@code en}. */
    protected String language;
    /** The book word count. */
    protected int wordCount;
    /** The book computed centrality. Can be {@code null} if not yet computed. */
    protected Double centrality;

    protected Book() {}

    public Book(int id, String title, String author, String language, int wordCount) {
        this(id, title, author, language, wordCount, null);
    }

    protected Book(int id, String title, String author, String language, int wordCount, Double centrality) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.language = language;
        this.wordCount = wordCount;
        this.centrality = centrality;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getLanguage() {
        return language;
    }

    public int getWordCount() {
        return wordCount;
    }

    public Double getCentrality() {
        return centrality;
    }

    public void setCentrality(double centrality) {
        this.centrality = centrality;
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
