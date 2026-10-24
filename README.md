# Zerno backend

Ktor + Exposed server for the Zerno Bible app. It serves Bibles stored as MyBible-style SQLite files.

## Running

| Task | What it does |
|---|---|
| `./gradlew test` | Route tests (`testApplication`) against small fixture databases, plus checks of the real meta database and bundled Bibles |
| `./gradlew run` | Starts the server on port 8080 (`PORT` overrides) |
| `./gradlew buildFatJar` | Builds a runnable JAR |
| `docker build -t zerno-backend .` | Builds the server image (JAR, public-domain Bibles, meta database, content) |

Config lives in `src/main/resources/application.yaml`:

- `bible.metaDbPath` (`BIBLE_META_DB_PATH`), default `databases/meta/meta.SQLite3`
- `bible.biblesDir` (`BIBLE_FILES_DIR`), default `databases/bibles`; a Bible file is read from `<biblesDir>/<language_id>/<db_path>`
- `content.dir` (`CONTENT_DIR`), default `content`; plans and achievements, see below
- `translations.showUnlicensed` (`SHOW_UNLICENSED_TRANSLATIONS`), default `false`; `true` lists translations whose licence isn't signed yet (NRT), for development only

## Deploying

`deploy/` runs one staging host with Docker Compose: the backend image behind Caddy, which gets a Let's Encrypt certificate (iOS refuses plain HTTP).

1. Point a DNS name at the host and open ports 80 and 443.
2. `cp deploy/.env.example deploy/.env` and set `ZERNO_DOMAIN`.
3. `docker compose -f deploy/compose.yaml up -d --build`
4. `curl https://<domain>/health` answers `OK`.

The image reads its paths from the same environment variables as above (`BIBLE_META_DB_PATH`, `BIBLE_FILES_DIR`, `CONTENT_DIR`, `PORT`), set to the files baked into `/app`. Licensed Bible files are not in the repository: mount a folder with them over `/app/databases/bibles` (see the commented volume in `compose.yaml`) and flip their `licensed` flag in the meta database when the licence allows. The app's release base URL is then `https://<domain>/`.

CI (`.github/workflows/ci.yml`) runs the tests and `validateBibles`, builds the image and checks `/health` in it.

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
| `GET /v1/translations` | `{"version", "translations": [...]}`: translations the app can download, see below |
| `GET /v1/translations/{id}/file` | The translation's SQLite file, with ranges |

Errors are JSON: `{"error": "bible_not_found", "message": "..."}` with 400 for bad input and 404 for an unknown language, Bible, book, or a Bible whose file isn't on the server.

The three `/v1` documents send the content `version` as an `ETag` with `Cache-Control: no-cache`; a request with a matching `If-None-Match` gets 304 and no body.

## Translations

`/v1/translations` lists every Bible in the meta database whose file is on the server and whose `licensed` flag is 1 (NRT and the Tajik Bible stay 0 until their licences allow it). Each entry has `id`, `translation_code`, `display_name`, `language_id`, `licence`, `attribution`, `schema_version` (the file layout, 1 for now), `file_version` (the first 16 characters of the checksum), `size`, `sha256`, `download_url` and `bundled` (the app ships KJV and RST). `download_url` is relative to the API base URL; resolve it rather than concatenating, so a storage URL can replace it later.

The file route sends the checksum as a strong `ETag`, `Accept-Ranges: bytes` and `Content-Disposition`. To resume, send `Range: bytes=<received>-` with `If-Range: "<sha256>"`: the server answers 206 with the rest, or 200 with the whole file if it changed meanwhile. A matching `If-None-Match` gets 304. Unknown, unlicensed and missing translations answer 404 `translation_not_found`. Files are read through `TranslationStorage`, local disk for now, so S3-compatible storage can replace it later. Checksums are worked out on first use and again when a file's size or date changes.

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
