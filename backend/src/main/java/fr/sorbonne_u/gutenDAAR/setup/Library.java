package fr.sorbonne_u.gutenDAAR.setup;

import fr.sorbonne_u.gutenDAAR.Settings;
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
 * Utility class for retrieving the texts (=book contents) from the mirror.
 *
 * <p>Its {@link #retrieveTreatedText(String)} method is in particular exposed.</p>
 */
public class Library {

    private Library() {}

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(Library.class);

    /** UNICODE_CHARACTER_CLASS so that \s also matches non-breaking spaces */
    protected static final Pattern WHITESPACES = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    /** The treated text content starts after the line accepting this pattern. */
    protected static final Pattern START_TEXT = Pattern.compile("^\\*\\*\\* START");
    /** The treated text content stops before the line accepting this pattern. */
    protected static final Pattern END_TEXT = Pattern.compile("^\\*\\*\\* END");

    /**
     * Retrieve the text from the mirror, and treat it for getting the exact text words.
     * @param pathWithinMirror          The path within the mirror, starting with {@code /}, leading to the text.
     * @return                          The treated text from the mirror.
     * @throws IOException              If an I/O error occurs while retrieving the text from the mirror.
     * @throws InterruptedException     If an interruption occurs while retrieving the text from the mirror.
     */
    public static List<String> retrieveTreatedText(String pathWithinMirror) throws IOException, InterruptedException {
        String rawText = retrieveText(pathWithinMirror);
        return treatRetrievedText(rawText);
    }

    /**
     * Retrieve the text from the mirror, having the path: {@link Settings#MIRROR_URL} + {@code pathWithinMirror}.
     * @param pathWithinMirror          The path within the mirror, from which the text is retrieved.
     * @return                          The text from the mirror
     * @throws IOException              If an I/O error occurs while retrieving the text from the mirror.
     * @throws InterruptedException     If an interruption occurs while retrieving the text from the mirror.
     */
    protected static String retrieveText(String pathWithinMirror) throws IOException, InterruptedException {
        URI mirrorURI = Settings.MIRROR_URL.resolve(pathWithinMirror);
        HttpRequest request = HttpRequest.newBuilder(mirrorURI)
                .header("User-Agent", Settings.USER_AGENT)
                .timeout(Duration.ofSeconds(Settings.RETRIEVE_TIMEOUT_SECONDS))
                .GET()
                .build();
        HttpResponse<String> response = Settings.CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Could not download " + mirrorURI.getPath() + ": " + response.statusCode());
        }

        LOG.debug("Retrieved the raw text from {}.", mirrorURI.getPath());
        return response.body();
    }

    /**
     * Treat the given raw text for getting the exact text words.
     * @param rawText       The text got after retrieval from the mirror.
     * @return              The exact text words.
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

        LOG.debug("Treated the raw text.");
        return words;
    }
}
