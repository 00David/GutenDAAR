import styles from '../css/Pagination.module.css'

interface PaginationProps {
  page: number
  size: number
  total: number
  onPage: (page: number) => void
}

/** Previous / next page buttons. Pages are numbered from 0 in the API, from 1 for the user. */
export default function Pagination({ page, size, total, onPage }: PaginationProps) {
  const pages = Math.ceil(total / size)
  if (pages <= 1) return null

  return (
    <nav className={styles.nav} aria-label="Pagination">
      <button type="button" className={styles.button} disabled={page <= 0} onClick={() => onPage(page - 1)}>
        ‹ Prev
      </button>
      <span className={styles.info}>
        Page {page + 1} / {pages}
      </span>
      <button type="button" className={styles.button} disabled={page >= pages - 1} onClick={() => onPage(page + 1)}>
        Next ›
      </button>
    </nav>
  )
}
