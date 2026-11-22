package fr.sorbonne_u.gutenDAAR.setup.retrieval;

import fr.sorbonne_u.gutenDAAR.setup.retrieval.GutenDAARCatalogCSV.GutenDAAREntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GutenDAARCatalogCSV} unit tests.
 */
class GutenDAARCatalogCSVTest {

    /** Title with a comma, authors with a comma. */
    private static final GutenDAAREntry TOM = new GutenDAAREntry(
            93,
            "Tom Sawyer, Detective",
            "Twain, Mark",
            "en",
            24550);
    /** Title with quotes, no author. */
    private static final GutenDAAREntry QUOTED = new GutenDAAREntry(
            1,
            "The \"Quoted\" Book",
            null,
            "fr",
            1200);

    /** The saved catalogs are written there, never in the real catalog. */
    @TempDir
    Path dir;

    @Test
    void parseTheSmallCatalog() throws IOException {
        // src/test/resources/catalog/catalog_small.csv: the 2 entries above, then a line whose word count is not a number
        try (Reader in = new InputStreamReader(
                GutenDAARCatalogCSVTest.class.getResourceAsStream("/catalog/catalog_small.csv"),
                StandardCharsets.UTF_8)) {
            assertEquals(List.of(TOM, QUOTED), GutenDAARCatalogCSV.parse(in));
        }
    }

    @Test
    void saveQuotesTheFieldsWhenNeeded() throws IOException {
        Path csv = dir.resolve("catalog.csv");
        GutenDAARCatalogCSV.save(List.of(TOM, QUOTED), csv);
        // fields with a comma or a quote are surrounded by quotes, inner quotes are doubled, null author is empty
        assertEquals(List.of(
                "Id,Title,Authors,Language,WordCount",
                "93,\"Tom Sawyer, Detective\",\"Twain, Mark\",en,24550",
                "1,\"The \"\"Quoted\"\" Book\",,fr,1200"), Files.readAllLines(csv));
    }

    @Test
    void saveThenParseGivesTheSameEntries() throws IOException {
        Path csv = dir.resolve("catalog.csv");
        GutenDAARCatalogCSV.save(List.of(TOM, QUOTED), csv);
        try (Reader in = Files.newBufferedReader(csv)) {
            assertEquals(List.of(TOM, QUOTED), GutenDAARCatalogCSV.parse(in));
        }
    }

    @Test
    void saveReplacesTheExistingFile() throws IOException {
        Path csv = dir.resolve("catalog.csv");
        GutenDAARCatalogCSV.save(List.of(TOM, QUOTED), csv);
        GutenDAARCatalogCSV.save(List.of(QUOTED), csv);
        try (Reader in = Files.newBufferedReader(csv)) {
            assertEquals(List.of(QUOTED), GutenDAARCatalogCSV.parse(in));
        }
        // the temporary file of the save does not remain
        try (var files = Files.list(dir)) {
            assertEquals(List.of(csv), files.toList());
        }
    }
}
