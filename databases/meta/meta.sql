-- Source of databases/meta/meta.SQLite3. After editing, rebuild it with:
--   rm databases/meta/meta.SQLite3 && sqlite3 databases/meta/meta.SQLite3 < databases/meta/meta.sql
-- MetaDatabaseTest fails if the two drift apart.

CREATE TABLE languages (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    local_name TEXT NOT NULL
);

CREATE TABLE bibles (
    id TEXT PRIMARY KEY,
    translation_code TEXT NOT NULL,
    display_name TEXT NOT NULL,
    language_id TEXT NOT NULL,
    licence TEXT NOT NULL,
    attribution TEXT NOT NULL,
    db_type TEXT NOT NULL,
    db_path TEXT NOT NULL,
    FOREIGN KEY(language_id) REFERENCES languages(id)
);

INSERT INTO languages (id, name, local_name) VALUES
    ('en', 'English', 'English'),
    ('ru', 'Russian', 'Русский'),
    ('tgk', 'Tajik', 'Забо́ни тоҷикӣ');

INSERT INTO bibles (id, translation_code, display_name, language_id, licence, attribution, db_type, db_path) VALUES
    ('kjv', 'KJV', 'King James Version', 'en', 'public-domain',
     'King James Version. Public domain (Crown copyright in the United Kingdom).', 'sqlite', 'kjv.SQLite3'),
    ('asv', 'ASV', 'American Standard Version', 'en', 'public-domain',
     'American Standard Version (1901). Public domain.', 'sqlite', 'asv.SQLite3'),
    ('rst', 'RST', 'Синодальный перевод', 'ru', 'public-domain',
     'Синодальный перевод (1876). Общественное достояние.', 'sqlite', 'rst.SQLite3'),
    ('nrt', 'NRT', 'Новый русский перевод', 'ru', 'proprietary',
     'Новый русский перевод © Biblica, Inc. Used by permission.', 'sqlite', 'nrt.SQLite3'),
    ('km', 'KM', 'Китоби Муқаддас (1992)', 'tgk', 'unknown',
     'Китоби Муқаддас (1992).', 'sqlite', 'km.SQLite3');
