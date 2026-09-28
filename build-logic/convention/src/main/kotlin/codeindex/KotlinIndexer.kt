package codeindex

/** One declaration (class, interface, object or function) found in a Kotlin source file. */
data class Symbol(
    val pkg: String,
    val name: String,
    val signature: String,
    val file: String,
    val summary: String?,
    val line: Int,
)

/**
 * Line-based scanner for Kotlin sources that finds declarations and their KDoc summaries (concept 17.5).
 *
 * It relies on the project's code style (one declaration per line start, KDoc directly above the
 * declaration and its annotations) instead of a full Kotlin parser.
 */
object KotlinIndexer {
    private val modifiers = setOf(
        "public", "private", "internal", "protected", "override", "open", "abstract", "final",
        "sealed", "data", "enum", "inline", "value", "inner", "suspend", "operator", "infix",
        "tailrec", "external", "annotation", "fun", "companion", "expect", "actual",
    )
    private val declaration = Regex("""^(?<mods>(?:[a-z]+\s+)*?)(?<kind>class|interface|object|fun)\b\s*(?<rest>.*)$""")
    private val packageLine = Regex("""^package\s+([\w.]+)""")

    /** Scans one file; `path` is the path shown in the index. */
    fun scan(path: String, source: String): List<Symbol> {
        val lines = source.lines()
        val pkg = lines.firstNotNullOfOrNull { packageLine.find(it.trim())?.groupValues?.get(1) } ?: ""
        val symbols = mutableListOf<Symbol>()
        val scopes = ArrayDeque<Pair<String, Int>>()
        var depth = 0
        var pendingDoc: String? = null
        var inDoc = false
        val docLines = mutableListOf<String>()
        var index = 0
        while (index < lines.size) {
            val raw = lines[index]
            val trimmed = raw.trim()
            when {
                inDoc -> {
                    docLines += trimmed
                    if (trimmed.endsWith("*/")) {
                        inDoc = false
                        pendingDoc = summaryOf(docLines)
                    }
                }
                trimmed.startsWith("/**") -> {
                    docLines.clear()
                    docLines += trimmed
                    if (trimmed.endsWith("*/") && trimmed.length > 4) pendingDoc = summaryOf(docLines) else inDoc = true
                }
                trimmed.startsWith("@") && !trimmed.contains(" fun ") && !trimmed.contains(" class ") -> Unit
                else -> {
                    val decl = parseDeclaration(stripAnnotations(trimmed))
                    if (decl != null) {
                        val (kind, name) = decl
                        val header = collectHeader(lines, index)
                        val owner = scopes.lastOrNull()?.first
                        val qualified = if (owner != null) "$owner.$name" else name
                        symbols += Symbol(pkg, qualified, signatureOf(header), path, pendingDoc, index + 1)
                        if (kind != "fun" && header.contains('{')) scopes.addLast(qualified to depth + 1)
                    }
                    pendingDoc = null
                }
            }
            if (!inDoc) {
                val delta = braceDelta(raw)
                depth += delta
                // A scope ends only when a closing brace drops below its level (not on its own header lines).
                if (delta < 0) {
                    while (scopes.isNotEmpty() && depth < scopes.last().second) scopes.removeLast()
                }
            }
            index++
        }
        return symbols
    }

    /** Returns (kind, name) if the line starts a named declaration, else null. */
    private fun parseDeclaration(line: String): Pair<String, String>? {
        val match = declaration.find(line) ?: return null
        val mods = match.groups["mods"]!!.value.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (mods.any { it !in modifiers }) return null
        var kind = match.groups["kind"]!!.value
        var rest = match.groups["rest"]!!.value
        if (kind == "fun" && rest.startsWith("interface ")) {
            kind = "interface"
            rest = rest.removePrefix("interface ").trimStart()
        }
        if (kind == "object" && mods.contains("companion") && !rest.first().isLetter()) return null
        if (kind == "fun" && rest.startsWith("(")) return null
        if (kind == "fun" && rest.startsWith("<")) rest = rest.substring(matchingAngle(rest) + 1).trimStart()
        val name = rest.takeWhile { it.isLetterOrDigit() || it == '_' || it == '.' || it == '`' || it == '<' || it == '>' || it == ',' }
            .replace(Regex("<[^<>]*>"), "")
            .substringBefore('<')
        return if (name.isEmpty() || name == "interface") null else kind to name.trim('`')
    }

    /** Index of the `>` that closes the leading `<`. */
    private fun matchingAngle(text: String): Int {
        var level = 0
        text.forEachIndexed { i, c ->
            if (c == '<') level++
            if (c == '>' && --level == 0) return i
        }
        return text.length - 1
    }

    /** Removes leading annotations written on the same line as the declaration. */
    private fun stripAnnotations(line: String): String =
        line.replace(Regex("""^(@[\w.]+(\([^)]*\))?\s+)+"""), "")

    /** Joins a declaration header that spans several lines until parentheses are balanced. */
    private fun collectHeader(lines: List<String>, start: Int): String {
        val builder = StringBuilder()
        var balance = 0
        var i = start
        while (i < lines.size) {
            val text = lines[i].trim()
            builder.append(if (builder.isEmpty()) text else " $text")
            balance += text.count { it == '(' } - text.count { it == ')' }
            if (balance <= 0) break
            i++
        }
        return builder.toString().replace(", )", ")").replace(",)", ")").replace("( ", "(").replace(" )", ")")
    }

    /** Signature = the header without annotations, body or expression body. */
    private fun signatureOf(header: String): String {
        var text = stripAnnotations(header)
        var level = 0
        for ((i, c) in text.withIndex()) {
            when (c) {
                '(', '<' -> level++
                ')' -> level--
                '>' -> if (text.getOrNull(i - 1) != '-') level--
                '{' -> if (level == 0) { text = text.substring(0, i); break }
                '=' -> if (level == 0 && text.getOrNull(i + 1) != '=' && text.getOrNull(i - 1) != '!') {
                    text = text.substring(0, i); break
                }
            }
        }
        return text.trim().removeSuffix(",").removeSuffix(":").trim()
    }

    /** First sentence line of a KDoc block, or null if it is empty. */
    private fun summaryOf(doc: List<String>): String? =
        doc.map { it.removePrefix("/**").removeSuffix("*/").trim().removePrefix("*").trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("@") }

    /** Net change of the brace depth on one line, ignoring braces in string and char literals. */
    private fun braceDelta(line: String): Int {
        var delta = 0
        var inString = false
        var quote = ' '
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inString && c == '\\' -> i++
                inString && c == quote -> inString = false
                inString -> Unit
                c == '"' || c == '\'' -> { inString = true; quote = c }
                c == '/' && line.getOrNull(i + 1) == '/' -> return delta
                c == '{' -> delta++
                c == '}' -> delta--
            }
            i++
        }
        return delta
    }
}
