package fr.sorbonne_u.gutenDAAR.api.controllers;

import fr.sorbonne_u.gutenDAAR.Settings;
import fr.sorbonne_u.gutenDAAR.api.dto.BookDTO;
import fr.sorbonne_u.gutenDAAR.database.services.BookService;
import fr.sorbonne_u.gutenDAAR.database.services.BookTextService;
import fr.sorbonne_u.gutenDAAR.search.RecommendationService;
import fr.sorbonne_u.gutenDAAR.setup.retrieval.covers.Covers;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * REST endpoints about a single book: details, cover, text and neighbours in the document graph.
 */
@RestController
@RequestMapping(Settings.BOOK_API_MAPPING)
public class BookController {

    /** The injected book service. */
    protected final BookService bookService;
    /** The injected recommendation service. */
    protected final RecommendationService recommendationService;
    /** The injected book text service. */
    protected final BookTextService bookTextService;

    public BookController(BookService bookService, RecommendationService recommendationService,
                          BookTextService bookTextService) {
        this.bookService = bookService;
        this.recommendationService = recommendationService;
        this.bookTextService = bookTextService;
    }

    /**
     * Get the details of a book.
     * @param id    The unique Gutenberg book id.
     * @return      The book details.
     * @throws ResponseStatusException  404 if the book is not in the database.
     */
    @GetMapping("/{id}")
    public BookDTO getBook(@PathVariable int id) {
        return bookService.findById(id)
                .map(BookDTO::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown book " + id));
    }

    /**
     * Get a book cover, as downloaded during the setup.
     * Never downloads anything at request time.
     * @param id    The unique Gutenberg book id.
     * @return      The JPEG cover, cached by the browser for a day.
     * @throws ResponseStatusException  404 if the book has no cover on disk.
     */
    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getCover(@PathVariable int id) {
        if (!Covers.isOnDisk(id, Settings.COVER_TYPE)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No cover for book " + id);
        }
        Path cover = Settings.COVER_TYPE.coverPath(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(new FileSystemResource(cover));
    }

    /**
     * Get the text of a book, without the Project Gutenberg header and footer,
     * so that books can be read even when the Gutenberg website is down.
     * @param id    The unique Gutenberg book id.
     * @return      The plain UTF-8 text, cached by the browser for a day.
     * @throws ResponseStatusException  404 if the book has no stored text.
     */
    @GetMapping("/{id}/text")
    public ResponseEntity<String> getText(@PathVariable int id) {
        String text = bookTextService.findBodyById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No text for book " + id));
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(text);
    }

    /**
     * Get the neighbours of a book in the document graph (G2), the closest first.
     * Same algorithm as the search recommendations, with this single book as source.
     * @param id        The unique Gutenberg book id.
     * @param limit     The maximum number of neighbours, from 1 to {@link Settings#MAX_PAGE_SIZE}.
     * @return          The neighbour books.
     * @throws ResponseStatusException  404 if the book is unknown, 400 if the limit is out of range.
     */
    @GetMapping("/{id}/neighbours")
    public List<BookDTO> getNeighbours(@PathVariable int id,
                                       @RequestParam(defaultValue = "" + Settings.MAX_RECOMMENDATIONS) int limit) {
        if (limit < 1 || limit > Settings.MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "limit must be between 1 and " + Settings.MAX_PAGE_SIZE + ".");
        }
        if (bookService.findById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown book " + id);
        }

        return recommendationService.recommend(Set.of(id), Set.of(id), limit);
    }
}
