package codeindex

import java.io.File

/** Result of one generator run: the two Markdown files and every problem found. */
data class CodeIndexResult(
    val codeIndex: String,
    val errorCodes: String,
    val problems: List<String>,
)

/** One entry of the ErrorCode enum. */
data class ErrorCodeEntry(
    val name: String,
    val code: String,
    val stringRes: String,
    val action: String,
    val description: String,
)

/** Builds docs/code-index.md and docs/error-codes.md from the app sources (concept 17.3, 17.5). */
object CodeIndex {
    private const val CODE_INDEX_HEADER = """# Code index – HAAC Android

> GENERATED FILE – do not edit by hand. Regenerate with `./gradlew codeIndex` (concept 17.5).

Lists every class and function of the app with signature, file and a one-line KDoc summary, grouped by package. Read it before writing code to reuse existing functions instead of duplicating them."""

    private const val ERROR_CODES_HEADER = """# Error codes – HAAC Android

> GENERATED FILE – do not edit by hand. Regenerated from `ErrorCode.kt` and `strings.xml` with `./gradlew codeIndex` (concept 17.3, 17.5).

Every error of the app carries one of these codes (format `HAAC-<AREA>-<NNN>`) and appears in the notification list (17.4).

| Code | User message | Action | Technical description |
| --- | --- | --- | --- |"""

    private val entry = Regex(
        """(\w+)\(\s*"(HAAC-[A-Z]+-\d{3})"\s*,\s*R\.string\.(\w+)\s*,\s*ErrorAction\.(\w+)\s*,\s*"((?:[^"\\]|\\.)*)"\s*,?\s*\)""",
    )
    private val stringRes = Regex("""<string name="(\w+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    /** Scans all `src/main` Kotlin sources below `root` and renders both files. */
    fun generate(root: File, errorCodeFile: File, errorStrings: File): CodeIndexResult {
        val problems = mutableListOf<String>()
        val symbols = sourceFiles(root).flatMap { file ->
            val rel = file.relativeTo(root).invariantSeparatorsPath
            KotlinIndexer.scan(rel, file.readText())
        }
        symbols.filter { it.summary == null }.forEach { problems += "${it.file}:${it.line}: ${it.name} has no KDoc summary" }
        val codes = if (errorCodeFile.exists()) parseErrorCodes(errorCodeFile.readText()) else emptyList()
        val strings = if (errorStrings.exists()) parseStrings(errorStrings.readText()) else emptyMap()
        return CodeIndexResult(renderIndex(symbols), renderErrorCodes(codes, strings, problems), problems)
    }

    /** Kotlin files of all modules' main source sets, excluding build output and build-logic. */
    fun sourceFiles(root: File): List<File> =
        root.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" && it.name != "build-logic" && !it.name.startsWith(".") }
            .filter { it.isFile && it.extension == "kt" && it.invariantSeparatorsPath.contains("/src/main/") }
            .sortedBy { it.invariantSeparatorsPath }
            .toList()

    /** Entries of the ErrorCode enum in source order. */
    fun parseErrorCodes(source: String): List<ErrorCodeEntry> =
        entry.findAll(source).map {
            val (name, code, res, action, description) = it.destructured
            ErrorCodeEntry(name, code, res, action, description.replace("\\\"", "\""))
        }.toList()

    /** `name -> text` of a strings.xml, with Android escapes removed. */
    fun parseStrings(xml: String): Map<String, String> =
        stringRes.findAll(xml).associate { it.groupValues[1] to it.groupValues[2].replace("\\'", "'").replace("\\\"", "\"").trim() }

    private fun renderIndex(symbols: List<Symbol>): String = buildString {
        append(CODE_INDEX_HEADER).append('\n')
        if (symbols.isEmpty()) append("\n_No code yet._\n")
        symbols.groupBy { it.pkg }.toSortedMap().forEach { (pkg, items) ->
            append("\n## ").append(pkg.ifEmpty { "(default package)" }).append("\n\n")
            append("| Symbol | Signature | File | Description |\n| --- | --- | --- | --- |\n")
            items.forEach { s ->
                append("| `").append(cell(s.name)).append("` | `").append(cell(s.signature)).append("` | `")
                    .append(s.file).append("` | ").append(cell(s.summary ?: "")).append(" |\n")
            }
        }
    }

    private fun renderErrorCodes(codes: List<ErrorCodeEntry>, strings: Map<String, String>, problems: MutableList<String>): String =
        buildString {
            append(ERROR_CODES_HEADER).append('\n')
            codes.forEach { c ->
                val message = strings[c.stringRes]
                if (message == null) problems += "${c.code}: string resource ${c.stringRes} not found"
                append("| ").append(c.code).append(" | ").append(cell(message ?: "")).append(" | ")
                    .append(c.action).append(" | ").append(cell(c.description)).append(" |\n")
            }
        }

    private fun cell(text: String): String = text.replace("|", "\\|").replace("\n", " ")
}
