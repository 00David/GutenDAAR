package fr.sorbonne_u.gutenDAAR.search;

/**
 * Indicate possible search modes.
 */
public enum SearchMode {
    /** Search by keyword, on indexed book words. */
    keyword,
    /** Search by regular expression, on indexed book words. */
    regex,
    /** Search by regular expression, on the full text of books. */
    fulltext;
}
