import { Link } from 'react-router'
import type { SearchResult } from '../api'
import { formatAuthor, formatCentrality, languageName, plural, splitTitle } from '../format'
import Cover from './Cover'
import styles from '../css/ResultRow.module.css'

interface ResultRowProps {
  result: SearchResult
  rank: number
}

/** One row of the search results: cover, title, author, language, and why it is ranked there. */
export default function ResultRow({ result, rank }: ResultRowProps) {
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
            <span className={styles.score}>{plural(result.score, 'occurrence')}</span>
            <span>centrality {formatCentrality(result.pagerankCentrality)}</span>
          </p>
        </div>
      </Link>
    </li>
  )
}
