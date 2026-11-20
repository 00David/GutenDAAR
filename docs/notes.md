# Design notes

Choices made during the project, and why (one or two lines each). Source material for the report.

## REST API

- **One endpoint per search algorithm** (`/api/search/keyword`, `/api/search/regex/words`, `/api/search/regex/fulltext`), all with the same JSON response. One algorithm = one URL = one service, so the architecture and the benchmarks map one to one to the API.
- **Postgres only does the lookup** (`postings WHERE term = ?`, via the inverted index); ranking, pagination and recommendations are computed in Java (package `search`). The algorithms stay ours, and the ranking can later combine tf-idf and centrality.
- **Keyword ranking (first version):** occurrences of the keyword in the book, descending; ties broken by book id so that pagination is stable.
- **Recommendations:** neighbours in G2 of the top 5 results, excluding every result, ordered by smallest distance to one of them, at most 10. Edges are precomputed, so they are sent in the same response as the results (~3 ms for a whole keyword search on 100 books).
- **Query normalization:** the keyword is lowercased like the indexed terms; must stay identical to the book tokenization.
- **CORS** on `/api/**`, GET only, origins configurable (`gutendaar.cors.allowed-origins`); backend listens on `0.0.0.0` for the LAN demo.

## Frontend styling: CSS Modules, no Tailwind (2026-11-03)
- Each component has its own `Component.module.css` next to `Component.tsx`; the JSX only uses `className={styles.x}`, so the components stay readable.
- CSS Modules scope class names per file (no collisions), built into Vite, no extra dependency.
- `src/index.css` holds the shared pieces: colour/font tokens (CSS variables), a small reset, `.sr-only`.
- Responsive layout with plain `@media` queries: phone < 40rem, tablet ≥ 40rem, desktop ≥ 64rem.
- Tailwind was removed (it was in the template): classes inside the JSX made the components hard to read.

## Search bar and ranking choice (2026-11-03)
- Search mode chosen in a dropdown inside the search bar (Keyword / Regex / Regex (text)).
- "Rank by" radio buttons under the bar: Occurrences (default), PageRank, Closeness, Betweenness. All shown, even before closeness and betweenness are computed.
- **One criterion at a time, no average.** Considered tick boxes with an average of max-normalised scores, rejected:
  - the normalisation is arbitrary (max-normalisation is crushed by one outlier book) and would need justifying;
  - the three centralities measure the same idea on the same graph and are correlated: averaging them says little, and the order becomes hard to explain;
  - one criterion at a time makes the demo and the report clear: switch the criterion and compare the orders.
  - If needed later: one extra option combining relevance and importance (e.g. occurrences × PageRank), with a single documented formula.
- Sent as one parameter: `/api/search/keyword?q=whale&rank=pagerank`; same in the page URL.
- TODO backend: read `rank`, compute closeness/betweenness offline (betweenness needs the θ threshold first, bug B2).

## Closeness centrality: harmonic, Dijkstra (2026-11-20)
- Graph: the jaccard k-NN graph (k = 10), same matrix as PageRank. Edge cost = jaccard distance (1 - similarity).
  Not the complete graph: jaccard distance is a metric, so on a complete graph every shortest path is the direct edge.
- Formula: harmonic closeness H(v) = Σ_{u≠v} 1/d(v,u) / (n-1), 1/∞ = 0 (Marchiori & Latora 2000; Boldi & Vigna 2014).
  Chosen over classic (n-1)/Σd because it stays defined if the 1664-book graph is not connected.
- Shortest paths: Dijkstra from each book (O(n·m log n)), not Floyd-Warshall (O(n³)): sparse graph, and Brandes reuses it.
- Edge cost floored at 1e-9 (ShortestPaths.MIN_COST): two identical vocabularies give distance 0, so 1/0.
- Sanity check on 100 books: top = English fiction (Wuthering Heights 0.81), bottom = German non-fiction (0.40).
  The largest language cluster (en, 45%) is favoured: to discuss in the report.

## Betweenness centrality: Brandes, weighted (2026-11-20)
- Same graph and costs as closeness (jaccard k-NN, cost = distance, floored at 1e-9: Brandes needs costs > 0).
- Brandes (2001): one Dijkstra per source also counting shortest paths (σ) and keeping predecessors, then dependencies
  accumulated from the farthest node back. O(n·m + n² log n). Shared Dijkstra: ShortestPaths.allFrom.
- Undirected: sum / 2. Normalized by (n-1)(n-2)/2 -> [0, 1], comparable between 100 and 1664 books (ranking unchanged).
- Ties: exact equality of path lengths (no epsilon). With real-valued distances, ties are practically absent, so σ ≈ 1;
  a float rounding difference can at worst give a whole pair to one path instead of 1/2 each.
- Sanity check on 100 books (Python oracle): top = books whose neighbours are in several languages
  (De mannen van '80, nl: nl/en/sv neighbours, 0.168), i.e. bridges between language clusters; 29/100 books at 0.
