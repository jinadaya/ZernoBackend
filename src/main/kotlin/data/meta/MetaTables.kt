package data.meta

import org.jetbrains.exposed.sql.Table

object MetaLanguagesTable : Table("languages") {
    val id = text("id")
    val name = text("name")
    val localName = text("local_name")
}

object MetaBiblesTable : Table("bibles") {
    val id = text("id")
    val displayName = text("display_name")
    val languageId = text("language_id")

    val dbType = text("db_type")
    val dbPath = text("db_path")
}