-- Book metadata
CREATE TABLE IF NOT EXISTS books (
    id           INT NOT NULL PRIMARY KEY, -- Gutenberg id
    title        TEXT NOT NULL,
    author       TEXT,
    language     TEXT NOT NULL,
    word_count   INT  NOT NULL,
    centrality    DOUBLE PRECISION
);

-- Forwarded index
CREATE TABLE IF NOT EXISTS postings (
    book_id      INT  NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    term         TEXT NOT NULL,
    occurrences  INT  NOT NULL,
    PRIMARY KEY (book_id, term) -- book → term
);
-- Inverted index (dynamically created once the postings table has been filled)
-- CREATE INDEX idx_postings_term ON postings(term); -- term → book

-- Jaccard graph (symmetrical)
CREATE TABLE IF NOT EXISTS jaccard_edges (
    book_a    INT  NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    book_b    INT  NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    distance  DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (book_a, book_b),
    CHECK (book_a <> book_b)
);