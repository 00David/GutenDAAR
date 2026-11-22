# GutenDAAR

Search engine over a library of 1,664 multilingual books from [Project Gutenberg](https://www.gutenberg.org/).  

Abdullah AL MAMUN  
David VADIMON  

The report is available [here](Rapport_DAAR_Projet2.pdf) (in french).

## Main features

- **Keyword search**: books containing a single word, through an inverted index.
- **RegEx search (indexed terms)**: books containing an indexed term (stemmed, lowercase) fully matching a regex.
- **RegEx search (full text)**: books whose original text has a line matching a regex (90 s timeout).
- **Ranking** of the results, by number of occurrences (keyword search only) or by centrality
  (closeness / betweenness / Pagerank) in the Jaccard graph of the books.
- **Recommendations**: books close to the top results in the Jaccard graph, shown with each search.
- **Book page**: metadata, cover, nearest neighbours in the graph, and link to Gutenberg.

## Deployment with Docker Compose

### First time

1. Create the database credentials in `secrets/` (see [secrets/README.md](secrets/README.md)):
   ```bash
   printf 'gutendaar' > secrets/db_name.txt
   printf 'daar'      > secrets/db_user.txt
   openssl rand -base64 24 | tr -d '\n' > secrets/db_password.txt
   ```
2. Build and start everything:
   ```bash
   docker compose up -d --build
   ```
3. Open http://localhost

   The first start builds the database (download of the books and covers, indexing, graph).  
   It can take some time with the real 1664 book catalog, so to prevent you from waiting too long, the default catalog used is its short version (only 100 books).  
   
   Meanwhile, the API answers `503 Service Unavailable`. Follow the progress with `docker compose logs -f backend`.

   Many book covers are not available on Gutenberg, which results in:
   - 404 warnings in the backend logs while the database is built;
   - silent 404 errors in the browser console while navigating the frontend.

   This is expected and handled in both cases (a placeholder cover is used instead): it does not affect the application.

**Only the frontend is published (port 80)**: the backend and the database are only reachable through the Docker network.

### Usefull commands

| Action                                     | Command                                                                              |
|--------------------------------------------|--------------------------------------------------------------------------------------|
| Rebuild only the backend, restart all      | `docker compose build backend && docker compose up -d`                               |
| Reset the database (rebuilt from scratch)  | `SPRING_PROFILES_ACTIVE=reset docker compose up -d --build`                          |
| Stop                                       | `docker compose down`                                                                |
| Stop and delete the data                   | `docker compose down -v`                                                             |

After a reset, run `docker compose up -d` once the setup is completed, so that the backend does not reset again
when it restarts.

## Frontend

React 19 + TypeScript, built with Vite, in [frontend/](frontend/).

- Pages: search (`/`), book (`/book/:id`) and reading (`/book/:id/read`).
- In production, served by nginx ([nginx.conf](frontend/nginx.conf)), which also forwards `/api/` to the backend.
- Development (Node 24, [pnpm](https://pnpm.io)), with `/api` forwarded to `http://localhost:8080`
  (override with `API_PROXY_TARGET`):
  ```bash
  cd frontend
  pnpm install
  pnpm dev
  ```

## Backend

Spring Boot 4 (Java 21, [Maven](https://maven.apache.org)), in [backend/](backend/).

Global settings are in [Settings.java](backend/src/main/java/fr/sorbonne_u/gutenDAAR/Settings.java).  
There you can especially rename `CATALOG_CSV_PATH`'s path to `data/catalog.csv`: `SPRING_PROFILES_ACTIVE=reset docker compose up -d --build` will then build the real 1664 book library.

- **REST API**:

  | Endpoint                                  | Description                               |
  |-------------------------------------------|-------------------------------------------|
  | `GET /api/search/keyword`                 | Keyword search                            |
  | `GET /api/search/regex/words`             | RegEx search on the indexed terms         |
  | `GET /api/search/regex/fulltext`          | RegEx search on the full text             |
  | `GET /api/books/{id}`                     | Book metadata                             |
  | `GET /api/books/{id}/cover`               | Book cover                                |
  | `GET /api/books/{id}/text`                | Text without Gutenberg header and footer  |
  | `GET /api/books/{id}/neighbours`          | Nearest books in the Jaccard graph        |

  Search parameters: `query`, `ranking` (`occurrences`, `pagerank`, `closeness`, `betweenness`),
  `page` (from 0) and `size` (1 to 100, default 20).
- **Setup** ([DataSetup.java](backend/src/main/java/fr/sorbonne_u/gutenDAAR/setup/DataSetup.java)), run at startup
  when the database is empty: catalog → books and covers → texts and postings → Jaccard graph
  (10 nearest neighbours per book) → Closeness / Betweenness / PageRank.
- **Local run**: copy `backend/.env.example` to `backend/.env`, fill in the credentials of a PostgreSQL database, then:
  ```bash
  cd backend
  ./mvnw spring-boot:run                                       # normal start
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=reset      # reset the database
  ```
- **Tests**: `./mvnw test` (needs a PostgreSQL database, as in the CI).
- **Benchmark** of the search times
  ([src/test/.../benchmark](backend/src/test/java/fr/sorbonne_u/gutenDAAR/benchmark/)), on a running server:
  `SearchBenchmark` writes `backend/benchmark/search_times.csv`, then `BenchmarkCharts` draws one chart per search mode.

## Database

PostgreSQL 17. The schema ([schema.sql](backend/src/main/resources/schema.sql)) is created at startup, and Hibernate
only validates it.

| Table           | Content                                                                         |
|-----------------|---------------------------------------------------------------------------------|
| `books`         | Metadata of each book (Gutenberg id, title, author, language, word count) and its centralities |
| `book_texts`    | Original full text of each book, apart from the metadata                        |
| `postings`      | Number of occurrences of each term in each book (forward index, book → term)    |
| `jaccard_edges` | Jaccard distance between neighbour books, stored in both directions             |

The inverted index (term → book) on `postings` is created once the table is filled, to speed up the insertions.
The distinct terms (vocabulary) are kept in memory for the RegEx search on indexed terms.
