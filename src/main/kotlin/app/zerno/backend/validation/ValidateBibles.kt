package app.zerno.backend.validation

import java.io.File
import kotlin.system.exitProcess

/**
 * `./gradlew validateBibles`: checks every `*.SQLite3` under the Bibles directory (first argument, default
 * `databases/bibles`) against schema v1, prints what it found, and writes `checksums.sha256` there in `sha256sum`
 * format. Exits with 1 when any file has an error.
 */
fun main(args: Array<String>) {
    val dir = File(args.firstOrNull() ?: "databases/bibles")
    val files = dir.walkTopDown().filter { it.isFile && it.name.endsWith(".SQLite3") }.sortedBy { it.path }.toList()
    require(files.isNotEmpty()) { "No .SQLite3 files under ${dir.absolutePath}" }

    val validator = BibleValidator()
    val reports = files.map(validator::validate)
    for (report in reports) {
        val name = report.file.relativeTo(dir).path
        println("${if (report.passes) "PASS" else "FAIL"} $name  ${report.sizeBytes} bytes  sha256 ${report.sha256}")
        report.errors.forEach { println("  error: $it") }
        report.warnings.forEach { println("  warning: $it") }
    }
    File(dir, "checksums.sha256").writeText(
        reports.joinToString("") { "${it.sha256}  ${it.file.relativeTo(dir).path}\n" },
    )
    if (reports.any { !it.passes }) exitProcess(1)
}
