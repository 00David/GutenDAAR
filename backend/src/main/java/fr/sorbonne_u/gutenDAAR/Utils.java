package fr.sorbonne_u.gutenDAAR;

import fr.sorbonne_u.gutenDAAR.setup.Stemmers;
import org.jspecify.annotations.Nullable;
import org.tartarus.snowball.SnowballStemmer;

import java.net.URI;
import java.util.Locale;

/**
 * Utility class.
 */
public final class Utils {

    private Utils() {}

    /**
     * Return a progress bar as a string, used for logging loading information.
     * @param done          The current progress.
     * @param total         The progress to reach.
     * @return              The bar filled with the current progress.
     */
    public static String progressBar(int done, int total) {
        int width = 30;
        int filled = done * width / total;
        return "[" + "#".repeat(filled) + ".".repeat(width - filled) + "] " + done + "/" + total;
    }

    /**
     * Convert a book id to its URI within the {@link Settings#MIRROR_URL} Gutenberg mirror.
     * @param id    A Gutenberg book id.
     * @return      Its text URI within the {@link Settings#MIRROR_URL} Gutenberg mirror.
     */
    public static URI textURIWithinMirror(int id) {
        String inMirror = id + "/pg" + id + ".txt";
        return Settings.MIRROR_URL.resolve(inMirror);
    }

    /**
     * Normalize a word like an indexed term (without stemming it).
     *
     * @param word          The word to normalize.
     * @return              The normalized word.
     */
    public static String normalize(String word) {
        return normalize(word, null);
    }

    /**
     * Normalize a word like an indexed term.
     *
     * @param word          The word to normalize.
     * @param stemmer       The word's language stemmer to use, or {@code null} if not provided.
     * @return              The normalized word.
     */
    public static String normalize(String word, @Nullable SnowballStemmer stemmer) {
        String w = word.strip().toLowerCase(Locale.ROOT);
        if (stemmer != null) {
            return Stemmers.stem(stemmer, w);
        } else {
            return w;
        }
    }

    /**
     * Separate a string according to {@link Settings#WORD_SEPARATORS}, used for getting
     * book text words.
     *
     * @param string      The string to separate.
     * @return            The separated words.
     */
    public static String[] separateWords(String string) {
        return Settings.WORD_SEPARATORS.splitAsStream(string)
                .filter(word -> !word.isEmpty())
                .toArray(String[]::new);
    }
}
