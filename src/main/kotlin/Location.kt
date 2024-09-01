/**
 * Represents the location of a parsed element within the source input.
 *
 * The `Location` class provides details about where a parsed element is located within the input source,
 * including the range of characters, the specific substring (lexeme), and the source itself. It also provides
 * functionality to retrieve the specific line of text from the source where the element is located.
 *
 * @property range The range of characters (as an [IntRange]) in the source where the element is located.
 * @property lexeme The substring (or lexeme) from the source that corresponds to the parsed element.
 * @property source The [Source] object representing the entire input source.
 */
data class Location(val range: IntRange, val lexeme: String, val source: Source) {

    /**
     * Retrieves the line of text from the source where the element is located.
     *
     * This function uses the current line number (based on the `Source` object) to fetch the corresponding
     * line of text from the source. It helps in locating the exact line in the source where the parsed element
     * appears, which can be useful for error reporting and debugging.
     *
     * @return The line of text from the source where the element is located.
     */
    fun getCurrentLine(): String {
        val lines = source.lines()
        val line = source.line()
        return lines[line]
    }
}
