package fr.sorbonne_u.gutenDAAR.database.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Removal of the Project Gutenberg header and footer.
 */
class BookTextServiceTest {

    @Test
    void keepsOnlyTheBookBetweenTheMarkers() {
        String content = """
                The Project Gutenberg eBook of Macbeth
                This ebook is for the use of anyone anywhere...

                *** START OF THE PROJECT GUTENBERG EBOOK MACBETH ***

                ACT I
                Thunder and lightning.

                *** END OF THE PROJECT GUTENBERG EBOOK MACBETH ***

                Updated editions will replace the previous one...
                """;
        assertEquals("ACT I\nThunder and lightning.", BookTextService.stripBoilerplate(content));
    }

    @Test
    void windowsLineEndingsAndOldMarkers() {
        String content = "Header\r\n***START OF THIS PROJECT GUTENBERG EBOOK X***\r\nText\r\n*** END OF THIS PROJECT GUTENBERG EBOOK X ***\r\nFooter";
        assertEquals("Text", BookTextService.stripBoilerplate(content));
    }

    @Test
    void missingMarkersKeepTheText() {
        assertEquals("Only text", BookTextService.stripBoilerplate("  Only text\n"));
        assertEquals("Text", BookTextService.stripBoilerplate("*** START OF X ***\nText\n"));
        assertEquals("Header\nText", BookTextService.stripBoilerplate("Header\nText\n*** END OF X ***\nFooter"));
    }
}
