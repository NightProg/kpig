

// the source file
data class Source(val content: String, var currentPos: Int = 0, private var whitespace: MutableList<Char> = mutableListOf()) {

    fun lines(): List<String> {
        return content.split("\n")
    }

    fun line(): Int {
        return content.substring(0, currentPos).count { it == '\n' }
    }

    fun column(): Int {
        return currentPos - content.substring(0, currentPos).lastIndexOf('\n')
    }

    fun save(): Source {
        return Source(content, currentPos, whitespace)
    }

    fun location(newSource: Source): Location {
        val substring = content.substring(currentPos, newSource.currentPos).trim(*whitespace.toCharArray())
        return Location(currentPos..newSource.currentPos, substring, this)
    }

    fun peek(): Char? {
        val c = content[currentPos]
        return if (whitespace.contains(c)) {
            advance(1)
        } else {
            c
        }
    }

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

    fun addWhitespace(ws: Char) = whitespace.add(ws)

    fun getWhitespace() = whitespace

    fun eof(): Boolean {
        return currentPos >= content.length
    }

    fun copy(): Source {
        return Source(content, currentPos, whitespace)
    }

}

