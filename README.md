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
- `content.dir` (`CONTENT_DIR`), default `content`; plans and achievements, see below

## Routes

| Route | Returns |
|---|---|
| `GET /health` | `OK` |
| `GET /languages` | All languages |
| `GET /bibles?langId=en` | Bibles of a language, with `translation_code`, `licence` and `attribution` |
| `GET /bible/{bibleId}` | Books; a book's `id` is its MyBible `book_number` (10 = Genesis) |
| `GET /bible/{bibleId}/{bookId}/chapters` | Chapters of a book with verse counts |
| `GET /bible/{bibleId}/verses?offset=0&limit=50` | Verses in reading order; `limit` is 1 to 1000, `total` is the whole Bible |
| `GET /v1/plans` | `{"version", "plans": [...]}`: reading plans in list order, each in the app's plan format |
| `GET /v1/achievements` | `{"version", "schema_version", "achievements": [...]}`: achievement definitions in the app's format |

Errors are JSON: `{"error": "bible_not_found", "message": "..."}` with 400 for bad input and 404 for an unknown language, Bible, book, or a Bible whose file isn't on the server.

Both `/v1` routes send the content `version` as an `ETag` with `Cache-Control: no-cache`; a request with a matching `If-None-Match` gets 304 and no body.

## Content

`content/` holds what the app can refresh without a release, in the same formats it bundles:

- `content/plans/index.json`: plan ids in the order the app lists them
- `content/plans/<id>.json`: one plan each (the app's `files/plans/<id>.json`)
- `content/achievements.json`: the achievement catalog (the app's `files/achievements.json`)

The server reads them once at start and refuses to start if a listed plan is missing, a plan's `id` doesn't match its file, a plan has no days, or an achievement code repeats. The `version` is a hash of the served documents, so editing a file and restarting is enough to publish it. A plan or achievement the app doesn't know how to show (new criteria type, unknown book) is the app's to skip, so keep new content within what released apps understand.

## Databases

- `databases/meta/meta.sql` is the source of `meta.SQLite3`. Edit the SQL, then rebuild:
  `rm databases/meta/meta.SQLite3 && sqlite3 databases/meta/meta.SQLite3 < databases/meta/meta.sql`
- Bible files are opened read-only and never modified by the server.
- Only public-domain texts are in this repository (KJV, ASV, RST). NRT is Biblica's copyrighted text and stays out until the licence is signed; the Tajik Bible (`km`) stays out until its licence is checked. Both are listed in the meta database and answer 404 `bible_file_missing` until their files are deployed.

### Checking Bible files

`./gradlew validateBibles` checks every `*.SQLite3` under `databases/bibles` against the v1 schema the app reads (`info`, `books`, `verses` with a unique index on book, chapter and verse; known book numbers, names and colours; non-empty verses; known markup tags) and writes `databases/bibles/checksums.sha256`. Errors fail the task. Warnings only report differences from the reference versification (KJV, or the Synodal one when `russian_numbering` is true), such as verses the translation leaves out, and spans that don't close. Run it before adding or replacing a translation file, and commit the updated checksums with it.
