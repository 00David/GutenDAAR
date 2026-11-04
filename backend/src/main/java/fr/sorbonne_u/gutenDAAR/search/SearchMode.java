package fr.sorbonne_u.gutenDAAR.search;

/**
 * Indicate possible search modes.
 */
public enum SearchMode {
    /** Search by keyword, on indexed book words. */
    KEYWORD,
    /** Search by regular expression, on indexed book words. */
    REGEX_WORDS,
    /** Search by regular expression, on all book words. */
    REGEX_FULL_TEXT
}
