package fr.sorbonne_u.gutenDAAR.setup.retrieval;

import fr.sorbonne_u.gutenDAAR.setup.retrieval.Library.RetrievedText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Library} unit tests.
 */
class LibraryTest {

    /** A raw book text whose content is the given lines, between Gutenberg header and footer. */
    private static String book(String... contentLines) {
        return "The Project Gutenberg eBook of Test\n"
                + "Header words\n"
                + "*** START OF THE PROJECT GUTENBERG EBOOK TEST ***\n"
                + String.join("\n", contentLines) + "\n"
                + "*** END OF THE PROJECT GUTENBERG EBOOK TEST ***\n"
                + "Footer words\n";
    }

    @Test
    void onlyTheContentBetweenTheMarkersIsKept() {
        RetrievedText t = Library.treatRetrievedText(book("Call me Ishmael"), "xx");
        // nothing from the header, the markers or the footer
        assertEquals(List.of("Call", "me", "Ishmael"), t.words());
    }

    @Test
    void punctuationAndSpacesSeparateTheWords() {
        RetrievedText t = Library.treatRetrievedText(book("Hello,   world!", "-- it's 1851."), "xx");
        assertEquals(List.of("Hello", "world", "it", "s", "1851"), t.words());
    }

    @Test
    void englishTermsAreLowercasedAndStemmed() {
        RetrievedText t = Library.treatRetrievedText(book("The cats are Running"), "en");
        // words: as written | terms: lowercase, then English stem
        assertEquals(List.of("The", "cats", "are", "Running"), t.words());
        assertEquals(List.of("the", "cat", "are", "run"), t.terms());
    }

    @Test
    void languageWithoutStemmerOnlyLowercasesTheTerms() {
        // no stemmer for "xx": "cats" stays "cats", accents are kept
        RetrievedText t = Library.treatRetrievedText(book("Cats CAFÉ"), "xx");
        assertEquals(List.of("cats", "café"), t.terms());
    }

    @Test
    void noStartMarkerGivesNoWord() {
        RetrievedText t = Library.treatRetrievedText("Some text\nwithout any marker\n", "en");
        assertTrue(t.words().isEmpty());
        assertTrue(t.terms().isEmpty());
    }

    @Test
    void rawTextIsKeptUnchanged() {
        String raw = book("Call me Ishmael");
        assertEquals(raw, Library.treatRetrievedText(raw, "en").rawText());
    }
}
