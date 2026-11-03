import styles from '../css/Status.module.css'

/** Loading indicator. */
export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className={styles.spinner} role="status">
      <span className={styles.wheel} />
      {label}
    </div>
  )
}

/** Error message box. */
export function ErrorBox({ message }: { message: string }) {
  return (
    <p className={styles.error} role="alert">
      {message}
    </p>
  )
}
