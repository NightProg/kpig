

data class Location(val range: IntRange, val lexeme: String, val source: Source) {
    fun getCurrentLine(): String {
        val lines = source.lines()
        val line = source.line()
        return lines[line]
    }
}

