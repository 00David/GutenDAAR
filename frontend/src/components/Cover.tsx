import { useState } from 'react'
import {apiUrl, PLACEHOLDER_COVER_TYPE, placeholderCoverPath} from '../api'
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
        <img
            src={placeholderCoverPath[PLACEHOLDER_COVER_TYPE]}
            alt={`Placeholder cover of ${title}`}
            loading="lazy"
            className={`${styles.image} ${className}`}
        />
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
