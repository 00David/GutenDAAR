/** Search options: the search modes (one per algorithm) and the ranking criteria. */

/** A search mode: one per search algorithm, each with its own endpoint. */
export interface SearchMode {
  key: string
  label: string
  endpoint: string
  /** Warning shown under the search bar when the mode is selected. */
  warning?: string
  placeholder: string
}

export const MODES: SearchMode[] = [
  {
    key: 'keyword',
    label: 'Keyword',
    endpoint: '/api/search/keyword',
    placeholder: 'A word, e.g. bonjour',
  },
  {
    key: 'regex',
    label: 'RegEx',
    endpoint: '/api/search/regex/words',
    placeholder: 'A pattern on words, e.g. wh(a|e)le',
  },
  {
    key: 'fulltext',
    label: 'RegEx (Full text)',
    endpoint: '/api/search/regex/fulltext',
    warning: 'Searches the full text of every book: it may take a while.',
    placeholder: 'A pattern on the text, e.g. white.whale',
  },
]

export const DEFAULT_MODE = MODES[0]

/** The mode of the given key, or the default mode if unknown. */
export function findMode(key: string | null): SearchMode {
  return MODES.find((m) => m.key === key) ?? DEFAULT_MODE
}

/** A ranking criterion: the results are sorted by one criterion at a time. */
export interface Ranking {
  key: string
  label: string
}

export const RANKINGS: Ranking[] = [
  { key: 'occurrences', label: 'Occurrences' }, // Only applicable to keyword search mode (findMode(keyword))
  { key: 'closeness', label: 'Closeness' },
  { key: 'betweenness', label: 'Betweenness' },
  { key: 'pagerank', label: 'PageRank' },
]

export const DEFAULT_RANKING = RANKINGS[0]

/** The ranking of the given key, or the default ranking if unknown. */
export function findRanking(key: string | null): Ranking {
  return RANKINGS.find((r) => r.key === key) ?? DEFAULT_RANKING
}

/** URL of the results page of a search, e.g. /?query=whale&mode=keyword&ranking=pagerank */
export function searchUrl(query: string, mode: string, ranking: string): string {
  return `/?${new URLSearchParams({ query: query, mode, ranking })}`
}
