package fr.sorbonne_u.gutenDAAR.benchmark;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.search.SearchMode;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;

/**
 * Draw the average server execution times written by {@link SearchBenchmark}: one bar chart per search mode,
 * with the request numbers on X and the average server execution time on Y.
 *
 * <p>Each chart is saved as a PNG next to the CSV, and also shown in a window if a display is available.</p>
 */
public final class BenchmarkCharts {

    private BenchmarkCharts() {}

    /** Bar orientation: VERTICAL puts the requests on X and the times on Y. */
    public static final PlotOrientation ORIENTATION = PlotOrientation.VERTICAL;
    /** Size of the PNG files, in pixels. */
    public static final int WIDTH = 900;
    public static final int HEIGHT = 500;
    /** Bar color. */
    public static final Color BAR_COLOR = new Color(0x3B6EA8);

    public static void main(String[] args) throws IOException {
        // Average times of each mode, by request number (in the CSV order)
        Map<SearchMode, DefaultCategoryDataset> datasets = new EnumMap<>(SearchMode.class);

        try (Reader reader = Files.newBufferedReader(SearchBenchmark.CSV_PATH);
             CSVParser parser = Settings.CSV_FORMAT.parse(reader)) {
            for (CSVRecord row : parser) {
                if (row.get("server_mean_ms").isEmpty()) {
                    System.out.println("Skipped " + row.get("id") + ": no successful run.");
                    continue;
                }
                SearchMode mode = SearchMode.valueOf(row.get("mode"));
                double mean = Double.parseDouble(row.get("server_mean_ms")) / unitInMs(mode);
                datasets.computeIfAbsent(mode, m -> new DefaultCategoryDataset())
                        .addValue(mean, "Mean server time", row.get("id"));
            }
        }

        for (Map.Entry<SearchMode, DefaultCategoryDataset> entry : datasets.entrySet()) {
            SearchMode mode = entry.getKey();
            JFreeChart chart = createChart(mode, entry.getValue());

            Path png = SearchBenchmark.CSV_PATH.resolveSibling("chart_" + mode + ".png");
            ChartUtils.saveChartAsPNG(png.toFile(), chart, WIDTH, HEIGHT);
            System.out.println("Chart saved to " + png.toAbsolutePath());

            if (!GraphicsEnvironment.isHeadless()) {
                ChartFrame frame = new ChartFrame(chart.getTitle().getText(), chart);
                frame.setSize(WIDTH, HEIGHT);
                frame.setVisible(true);
            }
        }
    }

    /** Create the bar chart of a search mode. */
    private static JFreeChart createChart(SearchMode mode, DefaultCategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(title(mode), "Request",
                "Mean server execution time (" + unit(mode) + ")", dataset, ORIENTATION, false, true, false);
        chart.setBackgroundPaint(Color.WHITE);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter()); // flat bars, without gradient
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, BAR_COLOR);
        // Value written above each bar
        renderer.setDefaultItemLabelGenerator(
                new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat(unitInMs(mode) == 1 ? "0.0" : "0.00")));
        renderer.setDefaultItemLabelsVisible(true);
        return chart;
    }

    /** Title of the chart of a search mode. */
    private static String title(SearchMode mode) {
        return switch (mode) {
            case keyword -> "Keyword search";
            case regex -> "RegEx search (indexed terms)";
            case regex_fulltext -> "RegEx search (full text)";
        };
    }

    /** Time unit of the chart of a search mode: seconds for the full text search, milliseconds otherwise. */
    private static String unit(SearchMode mode) {
        return mode == SearchMode.regex_fulltext ? "s" : "ms";
    }

    /** Number of milliseconds in the time unit of a search mode. */
    private static double unitInMs(SearchMode mode) {
        return mode == SearchMode.regex_fulltext ? 1000 : 1;
    }
}
