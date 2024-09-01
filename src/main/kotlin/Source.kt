/**
 * Represents the input source text and provides various methods for navigating and manipulating the text.
 *
 * The `Source` class maintains the content of the input text, tracks the current position within the text,
 * and provides methods to access lines, columns, and manage whitespace. It also includes methods to save the current
 * state and to handle whitespace characters.
 *
 * @property content The entire input text as a [String].
 * @property currentPos The current position within the content, represented as an [Int].
 * @property whitespace A mutable list of characters to be treated as whitespace.
 */
data class Source(
    val content: String,
    var currentPos: Int = 0,
    private var whitespace: MutableList<Char> = mutableListOf()
) {

    /**
     * Splits the content into lines based on newline characters.
     *
     * @return A list of strings where each string represents a line of text from the content.
     */
    fun lines(): List<String> {
        return content.split("\n")
    }

    /**
     * Calculates the current line number based on the current position.
     *
     * @return The line number (0-based) where the current position is located.
     */
    fun line(): Int {
        return content.substring(0, currentPos).count { it == '\n' }
    }

    /**
     * Calculates the current column number based on the current position.
     *
     * @return The column number (0-based) where the current position is located.
     */
    fun column(): Int {
        return currentPos - content.substring(0, currentPos).lastIndexOf('\n')
    }

    /**
     * Creates a copy of the current [Source] instance.
     *
     * @return A new [Source] instance with the same content, current position, and whitespace.
     */
    fun save(): Source {
        return Source(content, currentPos, whitespace)
    }

    /**
     * Creates a [Location] object representing the position between the current source and a new source.
     *
     * @param newSource The new [Source] instance to calculate the location range.
     * @return A [Location] object that represents the range between the current position and the new source position.
     */
    fun location(newSource: Source): Location {
        val substring = content.substring(currentPos, newSource.currentPos).trim(*whitespace.toCharArray())
        return Location(currentPos..newSource.currentPos, substring, this)
    }

    /**
     * Peeks at the next character in the content while skipping whitespace.
     *
     * @return The next non-whitespace character or `null` if the end of the content is reached.
     */
    fun peek(): Char? {
        val c = content[currentPos]
        return if (whitespace.contains(c)) {
            advance(1)
        } else {
            c
        }
    }

    /**
     * Advances the current position by a specified number of characters, skipping whitespace.
     *
     * @param i The number of characters to advance.
     * @return The next non-whitespace character or `null` if the end of the content is reached.
     */
    fun advance(i: Int): Char? {
        currentPos += i
        return if (eof()) {
            null
        } else {
            val c = content[currentPos]
            if (whitespace.contains(c)) {
                advance(1)
            } else {
                c
            }
        }
    }

    /**
     * Adds a character to the list of whitespace characters.
     *
     * @param ws The character to be treated as whitespace.
     */
    fun addWhitespace(ws: Char) = whitespace.add(ws)

    /**
     * Retrieves the list of whitespace characters.
     *
     * @return A list of characters that are treated as whitespace.
     */
    fun getWhitespace() = whitespace

    /**
     * Checks if the current position has reached or exceeded the end of the content.
     *
     * @return `true` if the current position is at or beyond the end of the content, `false` otherwise.
     */
    fun eof(): Boolean {
        return currentPos >= content.length
    }

    /**
     * Creates a copy of the current [Source] instance.
     *
     * @return A new [Source] instance with the same content, current position, and whitespace.
     */
    fun copy(): Source {
        return Source(content, currentPos, whitespace)
    }
}
