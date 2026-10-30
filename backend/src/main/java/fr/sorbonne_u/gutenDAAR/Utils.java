package fr.sorbonne_u.gutenDAAR;

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
}
