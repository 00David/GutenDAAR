import { Link } from 'react-router'
import type { Book } from '../api'
import { formatAuthor, splitTitle } from '../format'
import styles from '../css/BookCard.module.css'
import Cover from './Cover'

/** Small book card, for the recommendations and the similar books. */
export default function BookCard({ book }: { book: Book }) {
  const [title] = splitTitle(book.title)

  return (
    <Link to={`/book/${book.id}`} className={styles.card}>
      <Cover coverUrl={book.bookCoverEndpoint} title={title} className={styles.cover} />
      <div className={styles.text}>
        <p className={styles.title}>{title}</p>
        <p className={styles.author}>{formatAuthor(book.author)}</p>
      </div>
    </Link>
  )
}
