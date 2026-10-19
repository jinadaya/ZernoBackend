# Zerno backend

Ktor + Exposed server for the Zerno Bible app. It serves Bibles stored as MyBible-style SQLite files.

## Running

| Task | What it does |
|---|---|
| `./gradlew test` | Route tests (`testApplication`) against small fixture databases, plus checks of the real meta database and bundled Bibles |
| `./gradlew run` | Starts the server on port 8080 (`PORT` overrides) |
| `./gradlew buildFatJar` | Builds a runnable JAR |

Config lives in `src/main/resources/application.yaml`:

- `bible.metaDbPath` (`BIBLE_META_DB_PATH`), default `databases/meta/meta.SQLite3`
- `bible.biblesDir` (`BIBLE_FILES_DIR`), default `databases/bibles`; a Bible file is read from `<biblesDir>/<language_id>/<db_path>`

## Routes

| Route | Returns |
|---|---|
| `GET /health` | `OK` |
| `GET /languages` | All languages |
| `GET /bibles?langId=en` | Bibles of a language, with `translation_code`, `licence` and `attribution` |
| `GET /bible/{bibleId}` | Books; a book's `id` is its MyBible `book_number` (10 = Genesis) |
| `GET /bible/{bibleId}/{bookId}/chapters` | Chapters of a book with verse counts |
| `GET /bible/{bibleId}/verses?offset=0&limit=50` | Verses in reading order; `limit` is 1 to 1000, `total` is the whole Bible |

Errors are JSON: `{"error": "bible_not_found", "message": "..."}` with 400 for bad input and 404 for an unknown language, Bible, book, or a Bible whose file isn't on the server.

## Databases

- `databases/meta/meta.sql` is the source of `meta.SQLite3`. Edit the SQL, then rebuild:
  `rm databases/meta/meta.SQLite3 && sqlite3 databases/meta/meta.SQLite3 < databases/meta/meta.sql`
- Bible files are opened read-only and never modified by the server.
- Only public-domain texts are in this repository (KJV, ASV, RST). NRT is Biblica's copyrighted text and stays out until the licence is signed; the Tajik Bible (`km`) stays out until its licence is checked. Both are listed in the meta database and answer 404 `bible_file_missing` until their files are deployed.
