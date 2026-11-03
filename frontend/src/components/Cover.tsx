import { useState } from 'react'
import { apiUrl } from '../api'
import styles from '../css/Cover.module.css'

interface CoverProps {
  coverUrl: string
  title: string
  /** Class setting the size of the cover. */
  className?: string
}

/** Book cover, with a plain placeholder when the book has no cover. */
export default function Cover({ coverUrl, title, className = '' }: CoverProps) {
  const [failed, setFailed] = useState(false)

  if (failed) {
    return (
      <div className={`${styles.placeholder} ${className}`} aria-label={`No cover for ${title}`}>
        No cover
      </div>
    )
  }
  return (
    <img
      src={apiUrl(coverUrl)}
      alt={`Cover of ${title}`}
      loading="lazy"
      onError={() => setFailed(true)}
      className={`${styles.image} ${className}`}
    />
  )
}
