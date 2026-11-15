import { useId, useState, type SubmitEvent } from 'react'
import styles from '../css/SearchBar.module.css'
import { MODES, RANKINGS, findMode, findRanking } from '../modes'

interface SearchBarProps {
  initialQuery?: string
  initialMode?: string
  initialRanking?: string
  /** Large centred version for the home page. */
  large?: boolean
  onSearch: (query: string, mode: string, rank: string) => void
}

/** Search bar: mode dropdown, search input and button, and the choice of the ranking under it. */
export default function SearchBar({
  initialQuery = '',
  initialMode,
  initialRanking,
  large = false,
  onSearch,
}: SearchBarProps) {
  const [query, setQuery] = useState(initialQuery)
  const [modeKey, setModeKey] = useState(findMode(initialMode ?? null).key)
  const [rankKey, setRankKey] = useState(findRanking(initialRanking ?? null).key)
  const mode = findMode(modeKey)
  const rankLabelId = useId()

  /**
   * Indicates if the user can search, he cannot when:
   * - ranking by occurrences is selected
   * - and another search mode than by keyword is selected
   * @param givenRankKey   The rank hey to consider.
   */
  function canSearch(givenRankKey: string): boolean {
    return ! (givenRankKey === findRanking('occurrences').key
        && mode !== findMode('keyword'))
  }

  function submit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault()
    const q = query.trim()
    if (q) onSearch(q, mode.key, rankKey)
  }

  function chooseRank(key: string) {
    setRankKey(key)
    // on the results page, re-rank the current search right away
    const q = query.trim()
    if (!large && q) onSearch(q, mode.key, key)
  }

  return (
    <form onSubmit={submit} className={large ? `${styles.form} ${styles.large}` : styles.form} role="search">
      <div className={styles.row}>
        <div className={styles.field}>
          <select
            value={modeKey}
            onChange={(e) => setModeKey(e.target.value)}
            aria-label="Search mode"
            className={styles.select}
          >
            {MODES.map((m) => (
              <option key={m.key} value={m.key}>
                {m.label}
              </option>
            ))}
          </select>
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={mode.placeholder}
            aria-label="Search"
            autoFocus={large}
            autoCapitalize="off"
            autoCorrect="off"
            spellCheck={false}
            className={styles.input}
          />
        </div>
        <button type="submit" disabled={!query.trim() || !canSearch(rankKey)} className={styles.button}>
          Search
        </button>
      </div>

      <div role="radiogroup" aria-labelledby={rankLabelId} className={styles.ranks}>
        <span id={rankLabelId} className={styles.rankLabel}>
          Rank by
        </span>
        {RANKINGS.map((r) => (
          <label key={r.key} className={styles.rank}>
            <input
              type="radio"
              name="rank"
              value={r.key}
              checked={rankKey === r.key}
              onChange={() => chooseRank(r.key)}
              disabled={!canSearch(r.key)}
            />
            {r.label}
          </label>
        ))}
      </div>

      {mode.warning && <p className={styles.warning}>⚠️ {mode.warning}</p>}
    </form>
  )
}
