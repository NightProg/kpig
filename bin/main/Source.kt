

// the source file
data class Source(val content: String, private var currentPos: Int = 0) {
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
        return Source(content, currentPos)
    }

    fun location(oldSource: Source): Location {
        return Location(oldSource.currentPos..currentPos, content.substring(currentPos, oldSource.currentPos), this)
    }

    fun peek(): Char = content[currentPos]

    fun advance(i: Int): Char? {
        currentPos += i
        return if (eof()) {
            null
        } else {
            content[currentPos]
        }
    }

    fun eof(): Boolean {
        return currentPos >= content.length
    }


}

