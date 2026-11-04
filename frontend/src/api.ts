/**
 * Client of the GutenDAAR REST API.
 *
 * The API base URL is never hardcoded: by default it is empty, so requests go to the same origin
 * (the backend serving the built frontend, or the Vite dev proxy). For a frontend running on another
 * machine of the LAN, set VITE_API_URL (e.g. VITE_API_URL=http://192.168.1.20:8080).
 */
const API_URL: string = import.meta.env.VITE_API_URL ?? ''

/** Absolute URL of an API path. */
export function apiUrl(path: string): string {
  return API_URL + path
}

/** Gutenberg URL, used for inserting links targeting book descriptions. */
const GUTENBERG_URL:string = 'https://www.gutenberg.org/'

export function gutenbergBookUrl(id: number): string {
  return GUTENBERG_URL + 'ebooks/' + id
}

/** Default placeholder cover type. */
export const PLACEHOLDER_COVER_TYPE: CoverType = 'original'

type CoverType = 'small' | 'original'
export const placeholderCoverPath: Record<CoverType, string> = {
  small: '/covers/small-placeholder.jpg',
  original: '/covers/placeholder.jpg',
}

/** A book, as returned by /api/books/{id} and in the recommendations. */
export interface Book {
  id: number
  title: string
  author: string | null
  language: string
  wordCount: number
  closenessCentrality: number | null
  betweennessCentrality: number | null
  pagerankCentrality: number | null
  bookCoverEndpoint: string
  gutenbergUrl: string
}

/** One book of the search results. */
export interface SearchResult {
  id: number
  title: string
  author: string | null
  language: string
  closenessCentrality: number | null
  betweennessCentrality: number | null
  pagerankCentrality: number | null
  bookCoverEndpoint: string
  score: number
}

/** Response of every search endpoint. */
export interface SearchResponse {
  query: string
  mode: string
  timeMs: number
  total: number
  page: number
  size: number
  matchedWords: string[]
  results: SearchResult[]
  recommendations: Book[]
}

/** Error returned by the API, with the message to show to the user. */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(apiUrl(path), { signal })
  if (!response.ok) {
    // Spring's error body has a "message" field; fall back to the HTTP status
    let message = `Request failed (${response.status})`
    try {
      const body = await response.json()
      if (typeof body?.message === 'string' && body.message) message = body.message
    } catch {
      // body is not JSON: keep the default message
    }
    throw new ApiError(response.status, message)
  }
  return response.json() as Promise<T>
}

/** Run a search on the endpoint of the given mode, ranked by the given criterion. */
export function search(
  endpoint: string,
  query: string,
  rank: string,
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<SearchResponse> {
  const params = new URLSearchParams({ q: query, rank, page: String(page), size: String(size) })
  return getJson<SearchResponse>(`${endpoint}?${params}`, signal)
}

export function getBook(id: number, signal?: AbortSignal): Promise<Book> {
  return getJson<Book>(`/api/books/${id}`, signal)
}

export function getNeighbours(id: number, signal?: AbortSignal): Promise<Book[]> {
  return getJson<Book[]>(`/api/books/${id}/neighbours`, signal)
}
