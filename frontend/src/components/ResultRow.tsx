import { Link } from 'react-router'
import { getBookCentrality, type BookWithOccurrences } from '../api'
import { formatAuthor, formatCentrality, languageName, plural, splitTitle } from '../format'
import Cover from './Cover'
import styles from '../css/ResultRow.module.css'
import type { Ranking } from '../modes'

interface ResultRowProps {
  result: BookWithOccurrences
  ranking: Ranking
  rank: number
}

/** One row of the search results: cover, title, author, language, and why it is ranked there. */
export default function ResultRow({ result, ranking, rank }: ResultRowProps) {
  const [title, subtitle] = splitTitle(result.title)
  return (
    <li>
      <Link to={`/book/${result.id}`} className={styles.row}>
        <Cover bookId={result.id} title={title} className={styles.cover} />
        <div className={styles.body}>
          <h3 className={styles.title}>
            <span className={styles.rank}>{rank}.</span>
            {title}
          </h3>
          {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
          <p className={styles.meta}>
            {formatAuthor(result.author)} · {languageName(result.language)}
          </p>
          <p className={styles.stats}>
            {
              ranking.key === 'occurrences'
                ? <span className={styles.occurrences}>{plural(result.occurrences, 'occurrence')}</span>
                : <span className={styles.centrality}>{ranking.label} {formatCentrality(getBookCentrality(result, ranking))}</span>
            }
          </p>
        </div>
      </Link>
    </li>
  )
}
