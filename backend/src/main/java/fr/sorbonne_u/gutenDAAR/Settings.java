package fr.sorbonne_u.gutenDAAR;

import fr.sorbonne_u.gutenDAAR.setup.retrieval.covers.CoverType;
import org.apache.commons.csv.CSVFormat;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Define global settings for the backend application.
 */
public final class Settings {

    private Settings() {}

    /** Gutenberg URL. */
    public static final URI GUTENBERG_URL = URI.create("https://www.gutenberg.org/");

    /** Number of processed books between two progress bar logs. */
    public static final int PROGRESS_BAR_STEP = 20;

    /** Separators between words: anything that is not a letter (accents included) or a digit. */
    public static final Pattern WORD_SEPARATORS = Pattern.compile("[^\\p{L}\\p{M}\\p{N}]+");

    /* SETUP PART */

    /** User agent of the HTTP requests. */
    public static final String USER_AGENT = "GutenDAAR-student-project/1.0";
    /** Client of the HTTP requests. */
    public static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    /** Timeout of an HTTP request. */
    public static final int RETRIEVE_TIMEOUT_SECONDS = 60;
    /** URL of the Gutenberg CSV catalog. */
    public static final URI CATALOG_URL = URI.create("https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv");
    /** URL of the Gutenberg mirror. */
    public static final URI MIRROR_URL = URI.create("https://mirror.cs.odu.edu/gutenberg-epub/");
    /** Format of the CSV files, whose first line is the header. */
    public static CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
    /** Path of the GutenDAAR CSV catalog. */
    public static final Path CATALOG_CSV_PATH = Path.of("data/catalog.csv");
    /** Folder of the book covers. */
    public static final Path COVERS_FOLDER_PATH = Path.of("data/covers");
    /** Number of books of the GutenDAAR catalog, when created. */
    public static final int BOOKS = 1664;
    /** Rate of English books in the GutenDAAR catalog, when created. */
    public static final double ENG_BOOKS_RATE = 0.45;
    /** Minimum word count of a book of the GutenDAAR catalog, when created. */
    public static final int MIN_WORDS = 10000;
    /** Excluded languages, whose words are not separated by spaces. */
    public static final Set<String> EXCLUDED_LANGUAGES = Set.of("zh", "ja", "ko", "th", "lo", "km", "my", "bo");
    /** Rows per JDBC batch: must be the same as {@code spring.jpa.properties.hibernate.jdbc.batch_size}. */
    public static final int BATCH_SIZE = 1000;
    /** Book cover type to retrieve during setup. */
    public static final CoverType COVER_TYPE = CoverType.ORIGINAL;

    /* API PART */

    /** Book API mapping string. */
    public static final String BOOK_API_MAPPING = "/api/books";
    /** Search API mapping string. */
    public static final String SEARCH_API_MAPPING = "/api/search";

    /** Default number of results per page. */
    public static final int DEFAULT_PAGE_SIZE = 20;
    /** Maximum number of results per page. */
    public static final int MAX_PAGE_SIZE = 100;
    /** Number of top-ranked results whose graph neighbours are recommended. */
    public static final int RECOMMENDATION_SOURCES = 5;
    /** Maximum number of recommendations returned with a search. */
    public static final int MAX_RECOMMENDATIONS = 10;

    /* THREAD POOLS PART */

    /** Bean name of the executor scanning book indexed terms. */
    public static final String REGEX_EXECUTOR = "regexExecutor";
    /** Thread name prefix of the {@link #REGEX_EXECUTOR} threads, to recognize them in the logs. */
    public static final String REGEX_THREAD_PREFIX = "regex-";
    /** {@link #REGEX_EXECUTOR} number of threads. */
    public static final int REGEX_THREADS = Runtime.getRuntime().availableProcessors();
    /** {@link #REGEX_EXECUTOR} queue capacity, or {@code null} if not limited. */
    public static final Integer REGEX_QUEUE_CAPACITY = null;

    /** Bean name of the executor scanning book full texts. */
    public static final String FULL_TEXT_EXECUTOR = "fullTextExecutor";
    /** Thread name prefix of the {@link #FULL_TEXT_EXECUTOR} threads, to recognize them in the logs. */
    public static final String FULL_TEXT_THREAD_PREFIX = "full-text-";
    /** {@link #FULL_TEXT_EXECUTOR} number of threads. */
    public static final int FULL_TEXT_THREADS = Runtime.getRuntime().availableProcessors();
    /** {@link #FULL_TEXT_EXECUTOR} queue capacity, or {@code null} if not limited. */
    public static final Integer FULL_TEXT_QUEUE_CAPACITY = null;


}
