package fr.sorbonne_u.gutenDAAR.setup.retrieval;

import fr.sorbonne_u.gutenDAAR.setup.retrieval.GutenbergCatalogCSV.GutenbergEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link GutenbergCatalogCSV} unit tests.
 */
class GutenbergCatalogCSVTest {

    /** Authors with commas, several subjects. */
    private static final GutenbergEntry ALICE = new GutenbergEntry(
            11,
            "Text",
            "2008-06-27",
            "Alice's Adventures in Wonderland",
            "en",
            "Carroll, Lewis, 1832-1898",
            "Fantasy fiction; Fairy tales",
            "PR",
            "Category: Novels");
    /** Title with a comma, subjects with quotes, no bookshelf. */
    private static final GutenbergEntry TOM = new GutenbergEntry(
            93,
            "Text",
            "2004-02-01",
            "Tom Sawyer, Detective",
            "en",
            "Twain, Mark, 1835-1910",
            "Mystery; \"Sawyer, Tom\"",
            "PS",
            "");
    /** Title on two lines. */
    private static final GutenbergEntry MOBY = new GutenbergEntry(
            2701,
            "Text",
            "2001-07-01",
            "Moby Dick\nOr, The Whale",
            "en",
            "Melville, Herman, 1819-1891",
            "Whaling",
            "PS",
            "");
    /** Not a text, empty authors, subjects, LoCC and bookshelves. */
    private static final GutenbergEntry SOUND = new GutenbergEntry(
            10002,
            "Sound",
            "2003-12-01",
            "The House on the Borderland",
            "en",
            "",
            "",
            "",
            "");

    @Test
    void parseTheSmallCatalog() throws IOException {
        // src/test/resources/catalog/gutenberg_catalog_small.csv: the 4 entries above,
        // then a line whose id is not a number (ignored)
        try (Reader in = new InputStreamReader(
                GutenbergCatalogCSVTest.class.getResourceAsStream("/catalog/gutenberg_catalog_small.csv"),
                StandardCharsets.UTF_8)) {
            assertEquals(List.of(ALICE, TOM, MOBY, SOUND), GutenbergCatalogCSV.parse(in));
        }
    }

    @Test
    void notATextIsNotConverted() throws InterruptedException {
        // rejected on its type, before any download
        assertNull(SOUND.toGutenDAAREntry());
    }
}
