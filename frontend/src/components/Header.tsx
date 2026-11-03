import { Link, useNavigate } from 'react-router'
import styles from '../css/Header.module.css'
import { searchUrl } from '../modes'
import Logo from './Logo'
import SearchBar from './SearchBar'

interface HeaderProps {
  query?: string
  mode?: string
  rank?: string
}

/** Top bar with the logo and a compact search bar, on every page except the empty home. */
export default function Header({ query, mode, rank }: HeaderProps) {
  const navigate = useNavigate()

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link to="/" className={styles.logo}>
          <Logo />
        </Link>
        <SearchBar
          // remount when the URL changes (e.g. browser back), so the inputs show the current search
          key={`${query}|${mode}|${rank}`}
          initialQuery={query}
          initialMode={mode}
          initialRank={rank}
          onSearch={(q, m, r) => navigate(searchUrl(q, m, r))}
        />
      </div>
    </header>
  )
}
