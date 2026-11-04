package fr.sorbonne_u.gutenDAAR.setup.retrieval.covers;

import fr.sorbonne_u.gutenDAAR.Settings;

import java.net.URI;
import java.nio.file.Path;

/**
 * Define all possible book cover types.
 */
public enum CoverType {
    /** Small book cover. */
    SMALL,
    /** Original book cover. */
    ORIGINAL;

    /**
     * Convert a book id to its cover path on the disk.
     * @param bookId    A Gutenberg book id.
     * @return          Its cover path on the disk.
     */
    public Path coverPath(int bookId) {
        return switch (this) {
            case SMALL    -> Settings.COVERS_FOLDER_PATH.resolve("small_covers/" + bookId + ".jpg");
            case ORIGINAL -> Settings.COVERS_FOLDER_PATH.resolve("original_covers/" + bookId + ".jpg");
        };
    }

    /**
     * Convert a book id to its cover URI within the {@link Settings#MIRROR_URL} Gutenberg mirror.
     * @param bookId    A Gutenberg book id.
     * @return          Its cover URI within the {@link Settings#MIRROR_URL} Gutenberg mirror.
     */
    public URI coverURIWithinMirror(int bookId) {
        return switch (this) {
            case SMALL    -> Settings.MIRROR_URL.resolve(bookId + "/pg" + bookId + ".cover.small.jpg");
            case ORIGINAL -> Settings.MIRROR_URL.resolve(bookId + "/images/cover.jpg");
        };
    }
}
