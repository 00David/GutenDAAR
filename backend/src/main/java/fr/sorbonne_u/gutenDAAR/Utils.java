package fr.sorbonne_u.gutenDAAR;

import java.net.URI;

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
}
