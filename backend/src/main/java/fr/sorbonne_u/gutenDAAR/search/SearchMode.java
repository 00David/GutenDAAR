package fr.sorbonne_u.gutenDAAR.search;

/**
 * Indicate possible search modes.
 */
public enum SearchMode {
    /** Search by keyword, on indexed book terms. */
    KEYWORD,
    /** Search by regular expression, on indexed book terms. */
    REGEX_WORDS,
    /** Search by regular expression, on all book terms. */
    REGEX_FULL_TEXT
}
