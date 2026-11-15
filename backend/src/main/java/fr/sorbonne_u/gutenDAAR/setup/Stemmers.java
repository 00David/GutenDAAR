package fr.sorbonne_u.gutenDAAR.setup;

import fr.sorbonne_u.gutenDAAR.Utils;

import org.jspecify.annotations.Nullable;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.*;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Utility class for getting a stemmer corresponding to a given language.
 */
public final class Stemmers {

    private Stemmers() {}

    /** Association: language code -> its stemmer. */
    private static final Map<String, Supplier<SnowballStemmer>> BY_LANGUAGE = Map.ofEntries(
            Map.entry("ar", ArabicStemmer::new),
            Map.entry("ca", CatalanStemmer::new),
            Map.entry("da", DanishStemmer::new),
            Map.entry("de", GermanStemmer::new),
            Map.entry("el", GreekStemmer::new),
            Map.entry("en", EnglishStemmer::new),
            Map.entry("es", SpanishStemmer::new),
            Map.entry("et", EstonianStemmer::new),
            Map.entry("eu", BasqueStemmer::new),
            Map.entry("fi", FinnishStemmer::new),
            Map.entry("fr", FrenchStemmer::new),
            Map.entry("ga", IrishStemmer::new),
            Map.entry("hi", HindiStemmer::new),
            Map.entry("hu", HungarianStemmer::new),
            Map.entry("hy", ArmenianStemmer::new),
            Map.entry("id", IndonesianStemmer::new),
            Map.entry("it", ItalianStemmer::new),
            Map.entry("lt", LithuanianStemmer::new),
            Map.entry("ne", NepaliStemmer::new),
            Map.entry("nl", DutchStemmer::new),
            Map.entry("no", NorwegianStemmer::new),
            Map.entry("pt", PortugueseStemmer::new),
            Map.entry("ro", RomanianStemmer::new),
            Map.entry("ru", RussianStemmer::new),
            Map.entry("sr", SerbianStemmer::new),
            Map.entry("sv", SwedishStemmer::new),
            Map.entry("ta", TamilStemmer::new),
            Map.entry("tr", TurkishStemmer::new),
            Map.entry("yi", YiddishStemmer::new)
    );

    /**
     * Get the stemmer corresponding to the given language code,
     * or {@code null} if nothing is found.
     *
     * @param language      Language ISO code, for instance {@code en}.
     * @return              The corresponding language stemmer
     *                      or {@code null} if nothing is found.
     */
    public static @Nullable SnowballStemmer forLanguage(String language) {
        Supplier<SnowballStemmer> s = BY_LANGUAGE.get(language);
        return s == null ? null : s.get();
    }

    /**
     * The stem of an already normalized term, or the term itself if there is no stemmer.
     * See {@link Utils#normalize(String)} for term normalization.
     *
     * @param stemmer       The stemmer to use.
     * @param term          The normalized term to stem.
     * @return              The given term stemmed, or unmodified if no stemmer is provided.
     */
    public static String stem(@Nullable SnowballStemmer stemmer, String term) {
        if (stemmer == null) return term;
        stemmer.setCurrent(term);
        stemmer.stem();
        return stemmer.getCurrent();
    }
}