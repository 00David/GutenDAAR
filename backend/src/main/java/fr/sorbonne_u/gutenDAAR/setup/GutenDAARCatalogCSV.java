package fr.sorbonne_u.gutenDAAR.setup;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.Utils;
import fr.sorbonne_u.gutenDAAR.setup.GutenbergCatalogCSV.GutenbergEntry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for retrieving the CSV file containing GutenDAAR text metadata.
 *
 * <p>Its {@link #retrieve()} method is in particular exposed.</p>
 */
public class GutenDAARCatalogCSV {

    private GutenDAARCatalogCSV() {}

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(GutenDAARCatalogCSV.class);

    /** The header of the GutenDAAR CSV. */
    protected static final String[] HEADER = {"Id", "PathWithinMirror", "Title", "Authors", "Language", "WordCount"};

    /**
     * An entry of the GutenDAAR CSV catalog file (a line).
     * @param id                    The Gutenberg id of the text.
     * @param pathWithinMirror      The text file within the used {@link Settings#MIRROR_URL}.
     * @param title                 The text title.
     * @param authors               The text authors, might be {@code null}.
     * @param language              The text language ISO code, for instance {@code en}.
     * @param wordCount             The text word count.
     */
    public record GutenDAAREntry(int id, String pathWithinMirror, String title, String authors, String language, int wordCount) {}

    /**
     * Indicates if the GutenDAAR CSV catalog file, at {@link Settings#CATALOG_CSV_PATH}, exists.
     * @return  false if it is not there, or it is not a file.
     */
    public static boolean isOnDisk() {
        return Files.exists(Settings.CATALOG_CSV_PATH) && Files.isRegularFile(Settings.CATALOG_CSV_PATH);
    }

    /**
     * Retrieve the GutenDAAR catalog file, either by loading it from {@link Settings#CATALOG_CSV_PATH} if locally present,
     * or after building it from the retrieved Gutenberg catalog.
     * @return                  The catalog's entries, on the disk.
     * @throws IOException      If an error occurs while loading the file.
     */
    public static List<GutenDAAREntry> retrieve() throws IOException, InterruptedException {
        List<GutenDAAREntry> entries;
        if (isOnDisk()) {
            LOG.info("GutenDAAR CSV catalog already exists: getting its content.");
            try (Reader in = Files.newBufferedReader(Settings.CATALOG_CSV_PATH, StandardCharsets.UTF_8)) {
                entries = parse(in);
            }
        } else {
            LOG.info("GutenDAAR CSV catalog does not exist: retrieving and building it...");
            entries = retrieveAndBuild();
            save(entries);
        }
        return entries;
    }

    /**
     * Build the GutenDAAR catalog from the Gutenberg one, with {@link Settings#ENG_BOOKS_RATE} of English books.
     * @return                          The built GutenDAAR catalog's entries.
     * @throws IOException              If an IO error occurs while retrieving texts from the mirror.
     * @throws InterruptedException     If an interruption occurs while retrieving texts from the mirror.
     */
    protected static List<GutenDAAREntry> retrieveAndBuild() throws IOException, InterruptedException {
        int enQuota = (int) Math.round(Settings.BOOKS * Settings.ENG_BOOKS_RATE);
        int otherQuota = Settings.BOOKS - enQuota;
        int enBooks = 0;
        int otherBooks = 0;
        List<GutenDAAREntry> gutenDAAREntries = new ArrayList<>();

        LOG.info("Building CSV catalog {}", Utils.progressBar(gutenDAAREntries.size(), Settings.BOOKS));
        for (GutenbergEntry gbEntry : GutenbergCatalogCSV.retrieve()) {
            if (enBooks >= enQuota && otherBooks >= otherQuota) {
                break;
            }
            boolean isEnglish = gbEntry.language().equals("en");
            // checked on the Gutenberg catalog's metadata, before any download
            if (!gbEntry.type().equals("Text")
                    || gbEntry.language().isEmpty() || gbEntry.language().contains(";") // single language only
                    || Settings.EXCLUDED_LANGUAGES.contains(gbEntry.language())
                    || (isEnglish ? enBooks >= enQuota : otherBooks >= otherQuota)) {
                LOG.debug("Unavailable entry: {}", gbEntry);
                continue;
            }

            GutenDAAREntry gdEntry = gbEntry.toGutenDAAREntry(); // download the text corresponding to gbEntry's id

            if (gdEntry == null || gdEntry.wordCount() < Settings.MIN_WORDS) {
                LOG.debug("Book {} excluded while building GutenDAAR catalog.", gbEntry.id());
                continue;
            }
            gutenDAAREntries.add(gdEntry);
            if (isEnglish) {
                enBooks++;
            } else {
                otherBooks++;
            }

            if (gutenDAAREntries.size() % 100 == 0) {
                LOG.info("Building CSV catalog {}", Utils.progressBar(gutenDAAREntries.size(), Settings.BOOKS));
            }
        }

        LOG.debug("Built GutenDAAR catalog with {} entries, {} in English.", gutenDAAREntries.size(), enBooks);
        return gutenDAAREntries;
    }

    /**
     * Save the GutenDAAR catalog at {@link Settings#CATALOG_CSV_PATH}, replacing the existing file if any.
     * A {@code null} author is written as an empty field.
     * @param entries           The catalog's entries to save, written in this order.
     * @throws IOException      If an error occurs while writing or moving the file.
     */
    protected static void save(List<GutenDAAREntry> entries) throws IOException {
        Path dir = Settings.CATALOG_CSV_PATH.getParent();
        Files.createDirectories(dir);

        // write into a temporary file so that a failed save never leaves a truncated catalog
        Path tmp = Files.createTempFile(dir, "catalog", ".csv.part");
        try {
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(HEADER).get();
            try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(tmp, StandardCharsets.UTF_8), format)) {
                for (GutenDAAREntry e : entries) {
                    printer.printRecord(e.id(), e.pathWithinMirror(), e.title(), e.authors(), e.language(), e.wordCount());
                }
            }
            Files.move(tmp, Settings.CATALOG_CSV_PATH, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }

        LOG.debug("Saved GutenDAAR catalog at {}, with {} entries.", Settings.CATALOG_CSV_PATH, entries.size());
    }

    /**
     * Parse the GutenDAAR catalog file.
     * @param csv               The GutenDAAR CSV catalog read.
     * @return                  GutenDAAR catalog entries.
     * @throws IOException      If an error occurs while parsing the file.
     */
    protected static List<GutenDAAREntry> parse(Reader csv) throws IOException {
        List<GutenDAAREntry> entries = new ArrayList<>();
        for (CSVRecord r : Settings.CSV_FORMAT.parse(csv)) {
            try {
                String authors = r.get(HEADER[3]).isEmpty() ? null : r.get(HEADER[3]);
                entries.add(new GutenDAAREntry(
                        Integer.parseInt(r.get(HEADER[0])),
                        r.get(HEADER[1]),
                        r.get(HEADER[2]),
                        authors,
                        r.get(HEADER[4]),
                        Integer.parseInt(r.get(HEADER[5]))));
            } catch (NumberFormatException e) {
                LOG.warn("Ignored not well-formed record {} of the GutenDAAR catalog: {}", r.getRecordNumber(), e.getMessage());
            }
        }

        LOG.debug("Parsed GutenDAAR catalog, containing {} entries.", entries.size());
        return entries;
    }

    public static void main(String[] args) throws Exception {
        GutenDAARCatalogCSV d = new GutenDAARCatalogCSV();
        //
    }
}
