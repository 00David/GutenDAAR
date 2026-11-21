import { BrowserRouter, Route, Routes } from 'react-router'
import styles from './css/App.module.css'
import BookPage from './pages/BookPage'
import ReadPage from './pages/ReadPage'
import SearchPage from './pages/SearchPage'

function NotFound() {
  return <p className={styles.notFound}>Page not found.</p>
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<SearchPage />} />
        <Route path="/book/:id" element={<BookPage />} />
        <Route path="/book/:id/read" element={<ReadPage />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  )
}
