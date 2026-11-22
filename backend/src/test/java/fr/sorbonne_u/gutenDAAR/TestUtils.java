package fr.sorbonne_u.gutenDAAR;

import fr.sorbonne_u.gutenDAAR.setup.Stemmers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Utils} unit tests.
 */
public class TestUtils {

    @Test
    void testProgressBarEmpty() {
        assertEquals("[" + ".".repeat(30) + "] 0/100", Utils.progressBar(0, 100),
                "Progress bar should be empty");
    }

    @Test
    void testProgressBarHalf() {
        assertEquals("[" + "#".repeat(15) + ".".repeat(15) + "] 50/100", Utils.progressBar(50, 100),
                "Progress bar should be half-full");
    }

    @Test
    void testProgressBarFull() {
        assertEquals("[" + "#".repeat(30) + "] 100/100", Utils.progressBar(100, 100),
                "Progress bar should be full");
    }

    @Test
    void textURIWithinMirror() {
        assertEquals(Settings.MIRROR_URL.getPath()+"150/pg150.txt", Utils.textURIWithinMirror(150).getPath());
    }

    @Test
    void testNormalizeUniversity() {
        assertEquals("university", Utils.normalize("UnIVerSItY"));
    }

    @Test
    void testNormalizeAndStemUniversity() {
        assertEquals("univers", Utils.normalize("UnIVerSItY", Stemmers.forLanguage("en")));
    }

    @Test
    void testNormalizeAndStemUniversaly() {
        assertEquals("univers", Utils.normalize("UnIVerSAlLy", Stemmers.forLanguage("en")));
    }

    @Test
    void testSeparateWordsEmpty() {
        assertArrayEquals(new String[]{}, Utils.separateWords(""));
    }

    @Test
    void testSeparateWordsWithSpaces() {
        assertArrayEquals(new String[]{"a", "b"}, Utils.separateWords("a b"));
    }

    @Test
    void testSeparateWordsWithTrailingSpaces() {
        assertArrayEquals(new String[]{"a", "b"}, Utils.separateWords("  a b"));
    }

    @Test
    void testSeparateWordsWithSpecialSeparators() {
        assertArrayEquals(new String[]{"a", "b", "c", "d", "e", "8"}, Utils.separateWords("a,b''c-d^e!8"));
    }
}
