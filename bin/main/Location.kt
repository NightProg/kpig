

data class Location(val range: IntRange, val lexeme: String, val source: Source) {
    override fun toString(): String {
        return lexeme
    }
}