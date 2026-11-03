/**
 * Split a Gutenberg title: the subtitle comes after the first line break.
 * @returns the main title and the subtitle (empty if none)
 */
export function splitTitle(title: string): [string, string] {
  const i = title.indexOf('\n')
  return i < 0 ? [title, ''] : [title.slice(0, i).trim(), title.slice(i + 1).replace(/\s+/g, ' ').trim()]
}

const languageNames = new Intl.DisplayNames(['en'], { type: 'language' })

/** Full language name of a code, e.g. "fr" -> "French". */
export function languageName(code: string): string {
  try {
    return languageNames.of(code) ?? code
  } catch {
    return code
  }
}

/** Gutenberg authors look like "Doyle, Arthur Conan, 1859-1930": drop the dates. */
export function formatAuthor(author: string | null): string {
  if (!author) return 'Unknown author'
  return author
    .split(';')
    .map((a) => a.replace(/,\s*(\d{3,4}\??|BCE?|-|\s)+$/, '').trim())
    .join('; ')
}

export function formatCentrality(c: number | null): string {
  return c == null ? '–' : c.toPrecision(3)
}

export function plural(n: number, word: string): string {
  return `${n.toLocaleString('en')} ${word}${n === 1 ? '' : 's'}`
}
