import styles from '../css/SectionHeading.module.css'

interface SectionHeadingProps {
  /** Id of the heading, referenced by the section's aria-labelledby. */
  id: string
  title: string
  hint?: string
}

/** Small uppercase title of a section, with an optional explanation under it. */
export default function SectionHeading({ id, title, hint }: SectionHeadingProps) {
  return (
    <>
      <h2 id={id} className={styles.title}>
        {title}
      </h2>
      {hint && <p className={styles.hint}>{hint}</p>}
    </>
  )
}
