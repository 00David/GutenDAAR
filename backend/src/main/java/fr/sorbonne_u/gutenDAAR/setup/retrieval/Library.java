package fr.sorbonne_u.gutenDAAR.setup.retrieval;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for retrieving the books from the mirror.
 *
 * <p>
 * Its {@link #retrieveText(int)} method is in particular exposed.
 * </p>
 */
public class Library {

    private Library() {}

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(Library.class);

    /** Separators between words: anything that is not a letter (accents included) or a digit. */
    protected static final Pattern WORD_SEPARATORS = Pattern.compile("[^\\p{L}\\p{M}\\p{N}]+");
    /** The treated book text content starts after the line accepting this pattern. */
    protected static final Pattern START_TEXT = Pattern.compile("^\\*\\*\\* START");
    /** The treated book text content stops before the line accepting this pattern. */
    protected static final Pattern END_TEXT = Pattern.compile("^\\*\\*\\* END");

    /**
     * A book text, under its different forms.
     *
     * @param rawText   The original raw book text, as retrieved from the mirror.
     * @param words     The words of the text content, as they are written (not normalized).
     * @param terms     The terms of the text content, i.e. its indexed words.
     */
    public record RetrievedText(String rawText, List<String> words, List<String> terms) {}

    /**
     * Retrieve the book text from the mirror, with its words and terms.
     *
     * @param bookId                    The unique Gutenberg book id.
     * @return                          The raw book text, its words and its terms.
     * @throws IOException              If an I/O error occurs while retrieving the book from the mirror.
     * @throws InterruptedException     If an interruption occurs while retrieving the book from the mirror.
     */
    public static RetrievedText retrieveText(int bookId) throws IOException, InterruptedException {
        String rawText = retrieve(bookId);
        return treatRetrievedText(rawText);
    }

    /**
     * Retrieve the raw book text from the mirror (as a unique String).
     * @param bookId                    The unique Gutenberg book id.
     * @return                          The book text from the database or the mirror
     * @throws IOException              If an I/O error occurs while retrieving the book from the mirror.
     * @throws InterruptedException     If an interruption occurs while retrieving the book from the mirror.
     */
    protected static String retrieve(int bookId) throws IOException, InterruptedException {
        URI textURI = Utils.textURIWithinMirror(bookId);
        HttpRequest request = HttpRequest.newBuilder(textURI)
                .header("User-Agent", Settings.USER_AGENT)
                .timeout(Duration.ofSeconds(Settings.RETRIEVE_TIMEOUT_SECONDS))
                .GET()
                .build();
        HttpResponse<String> response = Settings.CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Could not download " + textURI.getPath() + ": " + response.statusCode());
        }

        LOG.debug("Retrieved the raw book {} text from {}.", bookId, textURI.getPath());
        return response.body();
    }

    /**
     * Treat the given raw book text for getting its words and terms.
     *
     * <p>Only the text content is considered, between the Gutenberg start and end markers.</p>
     *
     * @param rawText       The raw book text.
     * @return              The raw book text, its words and its terms.
     */
    protected static RetrievedText treatRetrievedText(String rawText) {
        List<String> lines = rawText.strip().lines().toList();
        List<String> words = new ArrayList<>();
        List<String> terms = new ArrayList<>();

        boolean textContentHasStarted = false;
        for (String line: lines) {
            // Only consider the lines for the "real" text content, not Gutenberg start marker or boilerplate
            if (textContentHasStarted) {
                if (END_TEXT.matcher(line).find()) {
                    break; // stop at the end marker line (not taking it into account)
                }

                for (String word : WORD_SEPARATORS.split(line)) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    words.add(word);

                    String term = Utils.normalize(word);
                    if (!term.isEmpty()) {
                        terms.add(term); // TODO: more selective strategy ?
                    }
                }
                // If the "real" text content has not started yet,
                // check if it will start (from the next line after the start marker)
            } else {
                if (START_TEXT.matcher(line).find()) {
                    textContentHasStarted = true;
                }
            }
        }

        LOG.debug("Treated the raw book text.");
        return new RetrievedText(rawText, words, terms);
    }
}
