import styles from '../css/Logo.module.css'

/** The "GutenDAAR" name. Its size is set by the parent. */
export default function Logo() {
  return (
    <span className={styles.logo}>
      Guten<span className={styles.accent}>DAAR</span>
    </span>
  )
}
