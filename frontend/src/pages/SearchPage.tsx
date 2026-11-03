import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { search, type SearchResponse } from '../api'
import BookCard from '../components/BookCard'
import Header from '../components/Header'
import Logo from '../components/Logo'
import Pagination from '../components/Pagination'
import ResultRow from '../components/ResultRow'
import SearchBar from '../components/SearchBar'
import SectionHeading from '../components/SectionHeading'
import { ErrorBox, Spinner } from '../components/Status'
import { plural } from '../format'
import { findMode, findRanking, searchUrl } from '../modes'
import styles from '../css/SearchPage.module.css'

const PAGE_SIZE = 20
/** Number of matched words shown in the stats line. */
const SHOWN_WORDS = 20

/** Result of the last request, tagged with the search it answers. */
interface Loaded {
  key: string
  data?: SearchResponse
  error?: string
}

/**
 * Home and results page. The search lives in the URL (?q=…&mode=…&rank=…&page=…), so the browser back button,
 * bookmarks and shared links all work.
 */
export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const query = params.get('q')?.trim() ?? ''
  const mode = findMode(params.get('mode'))
  const rank = findRanking(params.get('rank'))
  const page = Math.max(0, Number(params.get('page')) || 0)

  const key = `${mode.key}|${query}|${rank.key}|${page}`
  const [loaded, setLoaded] = useState<Loaded | null>(null)

  useEffect(() => {
    if (!query) return
    const controller = new AbortController()
    search(mode.endpoint, query, rank.key, page, PAGE_SIZE, controller.signal)
      .then((data) => setLoaded({ key, data }))
      .catch((e: unknown) => {
        if (!controller.signal.aborted) {
          setLoaded({ key, error: e instanceof Error ? e.message : 'Search failed.' })
        }
      })
    // a newer search cancels the previous request, so an old answer never overwrites a new one
    return () => controller.abort()
  }, [key, query, rank.key, page, mode.endpoint])

  const onSearch = (q: string, m: string, r: string) => navigate(searchUrl(q, m, r))

  // Home: nothing searched yet
  if (!query) {
    return (
      <main className={styles.home}>
        <h1 className={styles.homeTitle}>
          <Logo />
        </h1>
        <p className={styles.tagline}>Search engine over <i>Project Gutenberg</i> books</p>
        <SearchBar large initialMode={mode.key} initialRank={rank.key} onSearch={onSearch} />
      </main>
    )
  }

  const current = loaded?.key === key ? loaded : null
  const data = current?.data

  function goToPage(p: number) {
    const next = new URLSearchParams(params)
    next.set('page', String(p))
    setParams(next)
    window.scrollTo({ top: 0 })
  }

  return (
    <>
      <Header query={query} mode={mode.key} rank={rank.key} />
      <main className={styles.main}>
        {!current && <Spinner label="Searching…" />}
        {current?.error && <ErrorBox message={current.error} />}

        {data && (
          <>
            <p className={styles.stats}>
              <span className={styles.total}>{plural(data.total, 'book')}</span> · {data.timeMs.toFixed(1)} ms
              {data.matchedWords.length > 0 && (
                <>
                  {' '}
                  · matched:{' '}
                  <span className={styles.words}>
                    {data.matchedWords.slice(0, SHOWN_WORDS).join(', ')}
                    {data.matchedWords.length > SHOWN_WORDS && ` … (+${data.matchedWords.length - SHOWN_WORDS})`}
                  </span>
                </>
              )}
            </p>

            <div className={styles.layout}>
              <section className={styles.results} aria-labelledby="results-title">
                <SectionHeading id="results-title" title="Results" />
                {data.results.length === 0 ? (
                  <p className={styles.empty}>
                    No book contains <span className={styles.query}>{query}</span>.
                  </p>
                ) : (
                  <ol className={styles.list}>
                    {data.results.map((r, i) => (
                      <ResultRow key={r.id} result={r} rank={data.page * data.size + i + 1} />
                    ))}
                  </ol>
                )}
                <Pagination page={data.page} size={data.size} total={data.total} onPage={goToPage} />
              </section>

              {data.recommendations.length > 0 && (
                <aside className={styles.recommendations} aria-labelledby="recommendations-title">
                  <SectionHeading
                    id="recommendations-title"
                    title="You might also like"
                    hint="Books close to the top results in the document graph"
                  />
                  <div className={styles.cards}>
                    {data.recommendations.map((b) => (
                      <BookCard key={b.id} book={b} />
                    ))}
                  </div>
                </aside>
              )}
            </div>
          </>
        )}
      </main>
    </>
  )
}
