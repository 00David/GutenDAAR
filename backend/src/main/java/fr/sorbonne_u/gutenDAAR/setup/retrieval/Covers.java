package fr.sorbonne_u.gutenDAAR.setup.retrieval;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Utility class for retrieving the book covers from the mirror.
 *
 * <p>Its {@link #retrieveSmallCover(int)} method is in particular exposed.</p>
 */
public class Covers {

    private Covers() {}

    /** Logger. */
    protected static final Logger LOG = LoggerFactory.getLogger(Covers.class);

    /**
     * Retrieve the small book cover, either by loading it from the disk if locally present,
     * or after retrieving it from the mirror.
     * @return                          The raw bytes of the JPG book cover.
     * @throws IOException              If an error occurs while retrieving the file.
     * @throws InterruptedException     If an interruption occurs while retrieving the file.
     */
    public static byte[] retrieveSmallCover(int bookId) throws IOException, InterruptedException {
        Path smallCoverPath = Utils.smallCoverPath(bookId);
        byte[] cover;
        if (Files.exists(smallCoverPath) && Files.isRegularFile(smallCoverPath)) {
            return load(smallCoverPath);
        } else {
            cover = retrieve(Utils.smallCoverURIWithinMirror(bookId));
            save(cover, smallCoverPath);
        }
        return cover;
    }

    /**
     * Retrieve the book cover from the mirror.
     * @param coverURI                  The cover URI within {@link Settings#MIRROR_URL}.
     * @return                          The raw bytes of the JPG book cover.
     * @throws IOException              If an I/O error occurs while retrieving the book cover from the mirror.
     * @throws InterruptedException     If an interruption occurs while retrieving the book cover from the mirror.
     */
    protected static byte[] retrieve(URI coverURI) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(coverURI)
                .header("User-Agent", Settings.USER_AGENT)
                .timeout(Duration.ofSeconds(Settings.RETRIEVE_TIMEOUT_SECONDS))
                .GET()
                .build();
        HttpResponse<byte[]> response = Settings.CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Could not download " + coverURI.getPath() + ": " + response.statusCode());
        }

        LOG.debug("Retrieved the book cover from {}.", coverURI.getPath());
        return response.body();
    }

    /**
     * Save the given book cover on disk (replace the one already present, if there is).
     * @param cover                     The raw bytes of the JPG book cover to save.
     * @param coverPath                 The path where the cover is saved.
     * @throws IOException              If an I/O error occurs while saving the book cover.
     * @throws InterruptedException     If an interruption occurs while saving the book cover.
     */
    protected static void save(byte[] cover, Path coverPath) throws IOException {
        Files.createDirectories(coverPath.getParent());
        Files.write(coverPath, cover);
        LOG.debug("Saved the book cover at: {}.", coverPath);
    }

    /**
     * Load the book cover from the disk.
     * @param coverPath                 The path where the cover is saved.
     * @return                          The raw bytes of the JPG book cover.
     * @throws IOException              If an I/O error occurs while loading the book cover.
     */
    protected static byte[] load(Path coverPath) throws IOException {
        byte[] cover = Files.readAllBytes(coverPath);
        LOG.debug("Loaded the book cover from: {}.", coverPath);
        return cover;
    }
}
