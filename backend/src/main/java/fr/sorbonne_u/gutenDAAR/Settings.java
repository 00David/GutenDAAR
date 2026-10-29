package fr.sorbonne_u.gutenDAAR;

import org.apache.commons.csv.CSVFormat;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

/**
 * Define global settings for the backend application.
 */
public final class Settings {

    private Settings() {}

    /* SETUP PART */

    public static final String USER_AGENT = "GutenDAAR-student-project/1.0";
    public static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public static final int RETRIEVE_TIMEOUT_SECONDS = 60;
    public static final URI CATALOG_URL = URI.create("https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv");
    public static final URI MIRROR_URL = URI.create("https://mirror.cs.odu.edu/gutenberg-epub/");

    public static CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
    public static final Path CATALOG_CSV_PATH = Path.of("data/catalog_short.csv");
    public static final Path LIBRARY_FOLDER_PATH = Path.of("data/library");

    public static final int BOOKS = 1664;
    public static final double ENG_BOOKS_RATE = 0.45;
    public static final int MIN_WORDS = 10000;
    public static final Set<String> EXCLUDED_LANGUAGES = Set.of("zh", "ja", "ko", "th", "lo", "km", "my", "bo");

}
