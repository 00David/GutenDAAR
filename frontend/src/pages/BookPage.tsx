import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router'
import {getBook, getNeighbours, type Book, gutenbergBookUrl} from '../api'
import BookCard from '../components/BookCard'
import Cover from '../components/Cover'
import Header from '../components/Header'
import SectionHeading from '../components/SectionHeading'
import { ErrorBox, Spinner } from '../components/Status'
import { formatAuthor, formatCentrality, languageName, splitTitle } from '../format'
import styles from '../css/BookPage.module.css'

interface Loaded {
  id: number
  book?: Book
  neighbours?: Book[]
  error?: string
}

/** Details of a book, with its neighbours in the document graph. */
export default function BookPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const location = useLocation()
  const [loaded, setLoaded] = useState<Loaded | null>(null)

  useEffect(() => {
    if (!Number.isInteger(id)) return
    const controller = new AbortController()
    Promise.all([getBook(id, controller.signal), getNeighbours(id, controller.signal)])
      .then(([book, neighbours]) => setLoaded({ id, book, neighbours }))
      .catch((e: unknown) => {
        if (!controller.signal.aborted) {
          setLoaded({ id, error: e instanceof Error ? e.message : 'Could not load the book.' })
        }
      })
    return () => controller.abort()
  }, [id])

  const current = loaded?.id === id ? loaded : null
  const book = current?.book

  // Back to the previous page (the results) when we came from inside the app, otherwise to the home page
  const back = () => (location.key !== 'default' ? navigate(-1) : navigate('/'))

  return (
    <>
      <Header />
      <main className={styles.main}>
        <button type="button" onClick={back} className={styles.back}>
          ← Back
        </button>

        {!Number.isInteger(id) && <ErrorBox message="Invalid book id." />}
        {Number.isInteger(id) && !current && <Spinner />}
        {current?.error && <ErrorBox message={current.error} />}

        {book && <BookDetails book={book} />}

        {current?.neighbours && current.neighbours.length > 0 && (
          <section className={styles.similar} aria-labelledby="similar-title">
            <SectionHeading id="similar-title" title="Similar books" hint="Closest neighbours in the document graph" />
            <div className={styles.grid}>
              {current.neighbours.map((b) => (
                <BookCard key={b.id} book={b} />
              ))}
            </div>
          </section>
        )}
      </main>
    </>
  )
}

function BookDetails({ book }: { book: Book }) {
  const [title, subtitle] = splitTitle(book.title)

  return (
    <article className={styles.details}>
      <Cover coverUrl={book.bookCoverEndpoint} title={title} className={styles.cover} />
      <div className={styles.info}>
        <h1 className={styles.title}>{title}</h1>
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
        <p className={styles.author}>{formatAuthor(book.author)}</p>

        <dl className={styles.facts}>
          <dt>Language</dt>
          <dd>{languageName(book.language)}</dd>
          <dt>Words</dt>
          <dd>{book.wordCount.toLocaleString('en')}</dd>
          <dt>Centrality</dt>
          <dd>{formatCentrality(book.pagerankCentrality)}</dd>
          <dt>Gutenberg id</dt>
          <dd>{book.id}</dd>
        </dl>

        <a href={gutenbergBookUrl(book.id)} target="_blank" rel="noreferrer" className={styles.read}>
          Read on Gutenberg ↗
        </a>
      </div>
    </article>
  )
}
