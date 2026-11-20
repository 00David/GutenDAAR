package fr.sorbonne_u.gutenDAAR.benchmark;

import fr.sorbonne_u.gutenDAAR.Settings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Measure the response time of each {@link BenchmarkQuery}, and write the averages to {@link #CSV_PATH}.
 *
 * <p>
 * The server must be running, with its database fully set up. The requests are sent one after the other
 * (never concurrently), and each request is first sent {@link #WARMUP_RUNS} time(s) without being measured.
 * </p>
 *
 * <p>
 * The execution time is measured by the server({@code timeMs} field of the response).
 * The response time is measured by the client (HTTP round trip), also averaged, for comparison.
 * </p>
 *
 * <p>
 * Usage, from the {@code backend} folder: {@code java ... SearchBenchmark [baseUrl]},
 * {@code baseUrl} being {@value #DEFAULT_BASE_URL} by default (Docker Compose deployment, through nginx).
 * </p>
 */
public final class SearchBenchmark {

    private SearchBenchmark() {}

    /** Server URL used when none is given as argument. */
    public static final String DEFAULT_BASE_URL = "http://localhost";
    /** Generated CSV, relative to the working directory ({@code backend}). */
    public static final Path CSV_PATH = Path.of("benchmark/search_times.csv");
    /** CSV columns. */
    public static final String[] CSV_HEADER = {
            "id", "mode", "query", "ranking", "repetitions", "errors", "total_results",
            "mean_ms", "min_ms", "max_ms", "stddev_ms", "server_mean_ms"
    };
    /** Unmeasured runs sent before the measured ones, for each request. */
    public static final int WARMUP_RUNS = 1;
    /** Timeout of a request: above the server timeout of the full text search. */
    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(Settings.FULL_TEXT_SEARCH_TIMEOUT_SECONDS + 30);

    /** Time measured by the server, in the JSON response. */
    private static final Pattern SERVER_TIME = Pattern.compile("\"timeMs\"\\s*:\\s*([-0-9.eE+]+)");
    /** Total number of results, in the JSON response. */
    private static final Pattern TOTAL = Pattern.compile("\"total\"\\s*:\\s*(\\d+)");
    /** Format of the time in the logs. */
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * One measured run.
     * @param clientMs  Time measured by the client (HTTP round trip), in ms.
     * @param serverMs  Time measured by the server, in ms.
     * @param total     Total number of results.
     */
    private record Run(double clientMs, double serverMs, int total) {}

    public static void main(String[] args) throws IOException, InterruptedException {
        String baseUrl = args.length > 0 ? args[0].replaceAll("/+$", "") : DEFAULT_BASE_URL;
        List<BenchmarkQuery> queries = BenchmarkQuery.QUERIES;
        log("Benchmark of %d requests on %s", queries.size(), baseUrl);

        Files.createDirectories(CSV_PATH.getParent());
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
             Writer writer = Files.newBufferedWriter(CSV_PATH);
             CSVPrinter csv = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(CSV_HEADER).get())) {

            for (int q = 0; q < queries.size(); q++) {
                BenchmarkQuery query = queries.get(q);
                log("[%s] (%d/%d) %s '%s', ranking %s: %d warm-up + %d measured runs",
                        query.id(), q + 1, queries.size(), query.mode(), query.query(), query.ranking(),
                        WARMUP_RUNS, query.repetitions());

                for (int w = 0; w < WARMUP_RUNS; w++) {
                    try {
                        send(client, query, baseUrl);
                    } catch (IOException e) {
                        log("[%s]   warm-up failed: %s", query.id(), e.getMessage());
                    }
                }

                List<Run> runs = new ArrayList<>();
                int errors = 0;
                for (int r = 1; r <= query.repetitions(); r++) {
                    try {
                        Run run = send(client, query, baseUrl);
                        runs.add(run);
                        log("[%s]   run %d/%d: %.1f ms (server %.1f ms, %d results)",
                                query.id(), r, query.repetitions(), run.clientMs(), run.serverMs(), run.total());
                    } catch (IOException e) {
                        errors++;
                        log("[%s]   run %d/%d FAILED: %s", query.id(), r, query.repetitions(), e.getMessage());
                    }
                }

                writeRow(csv, query, runs, errors);
                csv.flush(); // the results already measured are kept if the benchmark is stopped
            }
        }
        log("Done: results written to %s", CSV_PATH.toAbsolutePath());
    }

    /**
     * Send a request and measure its response time.
     * @param client        The HTTP client.
     * @param query         The request to send.
     * @param baseUrl       The server URL.
     * @return              The measured run.
     * @throws IOException  If the request fails, or if the response status is not 200.
     */
    private static Run send(HttpClient client, BenchmarkQuery query, String baseUrl)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(query.uri(baseUrl)).timeout(REQUEST_TIMEOUT).GET().build();

        long start = System.nanoTime();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        double clientMs = (System.nanoTime() - start) / 1_000_000.0;

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return new Run(clientMs, extract(SERVER_TIME, response.body()), (int) extract(TOTAL, response.body()));
    }

    /** Extract a number from the JSON response. */
    private static double extract(Pattern field, String body) throws IOException {
        Matcher m = field.matcher(body);
        if (!m.find()) {
            throw new IOException("Field " + field + " not found in the response.");
        }
        return Double.parseDouble(m.group(1));
    }

    /** Write the statistics of a request in the CSV, and log them. */
    private static void writeRow(CSVPrinter csv, BenchmarkQuery query, List<Run> runs, int errors) throws IOException {
        if (runs.isEmpty()) {
            log("[%s] => no successful run", query.id());
            csv.printRecord(query.id(), query.mode(), query.query(), query.ranking(), query.repetitions(), errors,
                    "", "", "", "", "", "");
            return;
        }

        double mean = runs.stream().mapToDouble(Run::clientMs).average().orElseThrow();
        double min = runs.stream().mapToDouble(Run::clientMs).min().orElseThrow();
        double max = runs.stream().mapToDouble(Run::clientMs).max().orElseThrow();
        double variance = runs.size() < 2 ? 0 : runs.stream()
                .mapToDouble(r -> Math.pow(r.clientMs() - mean, 2))
                .sum() / (runs.size() - 1);
        double stddev = Math.sqrt(variance);
        double serverMean = runs.stream().mapToDouble(Run::serverMs).average().orElseThrow();
        int total = runs.getLast().total();

        log("[%s] => mean %.1f ms (min %.1f, max %.1f, stddev %.1f, server mean %.1f), %d results, %d error(s)",
                query.id(), mean, min, max, stddev, serverMean, total, errors);
        csv.printRecord(query.id(), query.mode(), query.query(), query.ranking(), query.repetitions(), errors, total,
                format(mean), format(min), format(max), format(stddev), format(serverMean));
    }

    /** Format a time in ms, with a dot as decimal separator. */
    private static String format(double ms) {
        return String.format(Locale.ROOT, "%.3f", ms);
    }

    /** Print a message on the console, with the current time. */
    private static void log(String format, Object... args) {
        System.out.println(LocalTime.now().format(LOG_TIME) + "  " + String.format(Locale.ROOT, format, args));
    }
}
