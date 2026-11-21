import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router'
import { getBook, getBookText, type Book } from '../api'
import Header from '../components/Header'
import { ErrorBox, Spinner } from '../components/Status'
import { formatAuthor, splitTitle } from '../format'
import styles from '../css/ReadPage.module.css'

interface Loaded {
  id: number
  book?: Book
  text?: string
  error?: string
}

/** The text of a book, served by our backend: books stay readable when the Gutenberg website is down. */
export default function ReadPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const location = useLocation()
  const [loaded, setLoaded] = useState<Loaded | null>(null)

  useEffect(() => {
    if (!Number.isInteger(id)) return
    const controller = new AbortController()
    Promise.all([getBook(id, controller.signal), getBookText(id, controller.signal)])
      .then(([book, text]) => setLoaded({ id, book, text }))
      .catch((e: unknown) => {
        if (!controller.signal.aborted) {
          setLoaded({ id, error: e instanceof Error ? e.message : 'Could not load the book.' })
        }
      })
    return () => controller.abort()
  }, [id])

  const current = loaded?.id === id ? loaded : null
  const book = current?.book

  // Back to the book page by going back in the history, so that its own "Back" still leads to the results:
  // opening the book page again would add it after the reader, and its "Back" would return here (loop).
  // When the reader was opened directly (shared link, reload), there is no history: open the book page.
  const back = () => (location.key !== 'default' ? navigate(-1) : navigate(`/book/${id}`, { replace: true }))

  return (
    <>
      <Header />
      <main className={styles.main}>
        <button type="button" onClick={back} className={styles.back}>
          ← Book details
        </button>

        {!Number.isInteger(id) && <ErrorBox message="Invalid book id." />}
        {Number.isInteger(id) && !current && <Spinner label="Loading the book…" />}
        {current?.error && <ErrorBox message={current.error} />}

        {book && current?.text !== undefined && (
          <article>
            <h1 className={styles.title}>{splitTitle(book.title)[0]}</h1>
            <p className={styles.author}>{formatAuthor(book.author)}</p>
            {/* plain text: original line breaks kept, long lines wrapped */}
            <div className={styles.text} lang={book.language}>
              {current.text}
            </div>
          </article>
        )}
      </main>
    </>
  )
}
