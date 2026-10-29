package fr.sorbonne_u.gutenDAAR.setup;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.setup.GutenDAARCatalogCSV.GutenDAAREntry;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for retrieving the CSV file containing Gutenberg text metadata.
 *
 * <p>Its {@link #retrieve()} method is in particular exposed.</p>
 */
public class GutenbergCatalogCSV {

    private GutenbergCatalogCSV() {}

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(GutenbergCatalogCSV.class);

    /** The header of the Gutenberg CSV. */
    protected static final String[] HEADER = {"Text#", "Type", "Issued", "Title", "Language", "Authors", "Subjects", "LoCC", "Bookshelves"};

    /**
     * An entry of the Gutenberg CSV catalog file (a line).
     * @param id                    The Gutenberg id of the text.
     * @param type                  The text type, for instance {@code Text}.
     * @param issued                The text date of publication, formatted as {@code YYYY-MM-DD}.
     * @param title                 The text title.
     * @param language              The text language ISO code, for instance {@code en}.
     * @param authors               The text authors.
     * @param subjects              The text subjects.
     * @param locc                  The Library of Congress Classification text code.
     * @param bookshelves           The text bookshelves.
     */
    public record GutenbergEntry(int id, String type, String issued, String title,
                                 String language, String authors, String subjects, String locc, String bookshelves) {

        /**
         * Return the equivalent of the current {@link GutenbergEntry}, as a {@link GutenDAAREntry},
         * or {@code null} if the entry isn't a text or an I/O exception occurs while retrieving
         * the treated text corresponding to the entry.
         *
         * @return                             The equivalent as a {@link GutenDAAREntry}, or {@code null}.
         * @throws InterruptedException        If an error occurs while retrieving the treated text corresponding to the entry.
         */
        public GutenDAAREntry toGutenDAAREntry() throws InterruptedException {
            if (!type.equals("Text")) {
                LOG.warn("Book {} isn't a text: conversion to GutenDAAREntry ignored.", id);
                return null;
            }

            try {
                String pathWithinMirror = idToPathWithinMirror(id);
                List<String> words = Library.retrieveTreatedText(pathWithinMirror);
                LOG.debug("Converted GutenbergEntry to GutenbergCatalogCSV for book {}", id);
                return new GutenDAAREntry(
                        id,
                        pathWithinMirror,
                        title,
                        authors.isEmpty() ? null : authors,
                        language,
                        words.size()
                );
            } catch (IOException e) {
                LOG.warn("Book {} unavailable: {}", id, e.getMessage());
                return null;
            }
        }

        /**
         * Convert a text id to its path within a Gutenberg mirror, i.e. an id of {@code 15}
         * is converted to {@code 15/pg15.txt}.
         * @param id    A text Gutenberg id.
         * @return      Its path within a Gutenberg mirror.
         */
        private static String idToPathWithinMirror(int id) {
            return id + "/pg" + id + ".txt";
        }
    }

    /**
     * Retrieve the Gutenberg CSV catalog from the mirror ({@link Settings#CATALOG_URL}).
     * @return                          The catalog's entries.
     * @throws IOException              If an I/O error occurs while retrieving the Gutenberg catalog.
     * @throws InterruptedException     If an interruption occurs while retrieving the Gutenberg catalog.
     */
    public static List<GutenbergEntry> retrieve() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(Settings.CATALOG_URL)
                .header("User-Agent", Settings.USER_AGENT)
                .timeout(Duration.ofSeconds(Settings.RETRIEVE_TIMEOUT_SECONDS))
                .GET()
                .build();
        HttpResponse<byte[]> response = Settings.CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Could not download pg_catalog.csv: " + response.statusCode());
        }
        byte[] bytes = response.body();
        try (Reader in = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            List<GutenbergEntry> entries = parse(in);
            LOG.debug("Retrieved Gutenberg CSV catalog.");
            return entries;
        }
    }

    /**
     * Parse the Gutenberg catalog file.
     * @param csv               The Gutenberg CSV catalog.
     * @return                  Gutenberg catalog entries.
     * @throws IOException      If an error occurs while parsing the file.
     */
    protected static List<GutenbergEntry> parse(Reader csv) throws IOException {
        List<GutenbergEntry> entries = new ArrayList<>();
        for (CSVRecord r : Settings.CSV_FORMAT.parse(csv)) {
            try {
                entries.add(new GutenbergEntry(
                        Integer.parseInt(r.get(HEADER[0])),
                        r.get(HEADER[1]),
                        r.get(HEADER[2]),
                        r.get(HEADER[3]),
                        r.get(HEADER[4]),
                        r.get(HEADER[5]),
                        r.get(HEADER[6]),
                        r.get(HEADER[7]),
                        r.get(HEADER[8])));
            } catch (NumberFormatException e) {
                // not well-formed line: ignored
            }
        }
        LOG.debug("Parsed Gutenberg catalog, containing {} entries.", entries.size());
        return entries;
    }
}
