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
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for retrieving the books from the mirror.
 *
 * <p>Its {@link #retrieveTreatedText(int)} method is in particular exposed.</p>
 */
public class Library {

    private Library() {}

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(Library.class);

    /** UNICODE_CHARACTER_CLASS so that \s also matches non-breaking spaces */
    protected static final Pattern WHITESPACES = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    /** The treated book text content starts after the line accepting this pattern. */
    protected static final Pattern START_TEXT = Pattern.compile("^\\*\\*\\* START");
    /** The treated book text content stops before the line accepting this pattern. */
    protected static final Pattern END_TEXT = Pattern.compile("^\\*\\*\\* END");

    /**
     * Retrieve the book text from the mirror, and treat it for getting the exact book words.
     * @param bookId                    The unique Gutenberg book id.
     * @return                          The treated book text from the mirror.
     * @throws IOException              If an I/O error occurs while retrieving the book from the mirror.
     * @throws InterruptedException     If an interruption occurs while retrieving the book from the mirror.
     */
    public static List<String> retrieveTreatedText(int bookId) throws IOException, InterruptedException {
        String rawText = retrieveText(bookId);
        return treatRetrievedText(rawText);
    }

    /**
     * Retrieve the book text from the mirror.
     * @param bookId                    The unique Gutenberg book id.
     * @return                          The book text from the mirror
     * @throws IOException              If an I/O error occurs while retrieving the book from the mirror.
     * @throws InterruptedException     If an interruption occurs while retrieving the book from the mirror.
     */
    protected static String retrieveText(int bookId) throws IOException, InterruptedException {
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

        LOG.debug("Retrieved the raw book text from {}.", textURI.getPath());
        return response.body();
    }

    /**
     * Treat the given raw book text for getting the exact book words.
     * @param rawText       The book text got after retrieval from the mirror.
     * @return              The exact book words.
     */
    protected static List<String> treatRetrievedText(String rawText) {
        List<String> lines = rawText.strip().lines().toList();
        List<String> words = new ArrayList<>();

        boolean textContentHasStarted = false;
        for (String line: lines) {
            // Only consider the lines for the "real" text content, not Gutenberg start marker or boilerplate
            if (textContentHasStarted) {
                if (END_TEXT.matcher(line).find()) {
                    break; // stop at the end marker line (not taking it into account)
                }

                for (String word : WHITESPACES.split(line)) {
                    if (!word.isEmpty()) {
                        words.add(word.toLowerCase(Locale.ROOT));
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
        return words;
    }
}
