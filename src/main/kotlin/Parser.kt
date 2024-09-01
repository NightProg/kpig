import java.util.regex.Pattern

/**
 * A Parse Exception is thrown when a parser encounters an error.
 * @param loc The location of the error
 * @param problem The range of the problem
 * @param message The error message
 * @constructor Creates a new parse exception
 * @property loc The location of the error
 * @property problem The range of the problem
 * @property message The error message
 * @see ParseException.Companion.formatError

 */
class ParseException(val loc: Location, val problem: IntRange, override val message: String): Exception(formatError(loc, problem, message)) {
    companion object {
        /**
         * Formats an error message
         * @param loc The location of the error
         * @param problem The range of the problem
         * @param message The error message
         * @return The formatted error message
         */
        fun formatError(loc: Location, problem: IntRange, message: String): String {
            var msg = "Error at ${loc.source.line()}:${loc.source.column()}: $message"
            msg += "\n${loc.getCurrentLine()}"
            msg += "\n" + " ".repeat(problem.first) + "^".repeat(problem.last - problem.first)
            return msg
        }
    }

}

/**
 * A Type alias for a parser function
 * @param T The return type of the parser
 */
typealias ParseFunc<T> = Parser.() -> T

/**
 * A Parse Function Class that wraps a parser function
 * @param T The return type of the parser
 * @property f The parser function
 * @property name The name of the parser function (optional)
 * @property canBeIgnored Whether the result of the parser can be ignored
 * @constructor Creates a new parse function
 */
class ParseFn<T>(val f: ParseFunc<T>, val name: String? = null, val canBeIgnored: Boolean = false) {
    /**
     * Invokes the parser function
     * @param parser The parser
     * @return The result of the parser
     */
    operator fun invoke(parser: Parser): T {
        return f(parser)
    }

    /**
     * Maps the result of the parser function
     * @param f The mapping function
     * @return The new parser function
     */
    infix fun<R> map(f: (T) -> R): ParseFn<R> {
        val obj = this
        return ParseFn({
            f(obj.invoke(this))
        })
    }

    /**
     * Chains the result of the parser function with another parser function
     * @param R The return type of the next parser function
     * @param f The next parser function
     * @return The new parser function that chains the two parsers together and returns a pair
     */
    infix fun<R> then(f: ParseFn<R>): ParseFn<Pair<T, R>> {
        val obj = this
        return ParseFn({
            Pair(obj.invoke(this), f.invoke(this))
        })
    }

    /**
     * Chains the result of the parser function with another parser function and ignores the result of the second parser
     * @param R The return type of the next parser function
     * @param f The next parser function
     * @return The new parser function that chains the two parsers together and returns the result of the first parser
     */
    infix fun<R> thenIgnore(f: ParseFn<R>): ParseFn<T> {
        val obj = this
        return ParseFn({
            val r = obj.invoke(this)
            f.invoke(this)
            r
        })
    }

    /**
     * Chains the result of the parser function with another parser function
     * @param R The return type of the next parser function
     * @param f The next parser function
     * @return The new parser function that chains the two parsers together and returns the result of the second parser
     */
    infix fun<R> rthen(f: ParseFn<R>): ParseFn<R> {
        val obj = this
        return ParseFn({
            obj.invoke(this)
            f.invoke(this)
        })
    }

    /**
     * Tries to apply the first parser function, if it fails, applies the second parser function
     * @param f The second parser function
     * @return The new parser function that tries the first parser and falls back to the second parser
     */
    infix fun or(f: ParseFn<T>): ParseFn<T> {
        val obj = this
        return ParseFn({
            choices(obj, f).invoke(this)
        }, "(${obj.name ?: "<unknown>"}, ${f.name ?: "<unknown>"})")
    }

    /**
     * name the parser function
     * @param name The name of the parser function
     * @return The new parser function with the name
     */
    infix fun named(name: String): ParseFn<T> {
        return ParseFn(f, name)
    }

    /**
     * Catches a ParseException and applies a recovery function
     * @param f The recovery function
     * @return The new parser function that catches exceptions and applies the recovery function
     */
    infix fun catch(f: (ParseException) -> T): ParseFn<T> {
        val obj = this
        return ParseFn({
            try {
                obj.invoke(this)
            } catch (x: ParseException) {
                f(x)
            }
        })
    }
    /**
     * Makes the parser function optional, returning a default value if it fails
     * @param defaultValue The default value to return if the parser function fails (null by default)
     * @return The new parser function that returns a nullable value
     */
    fun optional(defaultValue: T? = null): ParseFn<T?> {
        val obj = this
        return ParseFn({
            try {
                obj.invoke(this)
            } catch (x: ParseException) {
                defaultValue
            }
        })
    }
}

/**
 * A Parser class that wraps a source and provides parsing utility functions
 * @property source The source to parse
 * @constructor Creates a new parser
 */
class Parser(var source: Source) {

    /**
     * Parses a specific string v from the input source.
     *
     * @param v The string to be matched from the input source.
     * @return A [ParseFn] function that parses the given string v from the input source and returns its location.
     * @throws ParseException if the input source does not contain the expected string v or reaches the end of input prematurely.
     */
    fun tag(v: String): ParseFn<Location> {
        return ParseFn({
            val saved = source.save();
            for (c in v) {
                if (source.eof()) {
                    throw ParseException(saved.location(source), saved.location(source).range, "expected $v found eof")
                }
                if (source.peek() != c) {
                    val loc = saved.location(source)
                    throw ParseException(loc, loc.range, "expected $v found ${loc.lexeme}")
                }
                source.advance(1)
            }
            saved.location(source)
        }, v)
    }

    /**
     * Parses a specific regex regex from the input source.
     * @param regex The regex to be matched from the input source.
     * @return A [ParseFn] function that parses the given regex regex from the input source and returns its location.
     */
    fun regex(regex: String): ParseFn<Location> {
        return ParseFn({
            val pattern = Pattern.compile(regex).toRegex()
            val src = source.copy()
            val saved = source.save()
            var i = 0
            if (src.eof()) {
                throw ParseException(saved.location(source), saved.location(source).range, "eof reached")
            }
            var s = src.peek().toString()
            var hasBennMatched = false;
            while (true) {
                if (src.eof()) {
                    if (hasBennMatched) {
                        break
                    }
                    throw ParseException(saved.location(source), saved.location(source).range, "eof reached")
                }

                if (pattern.matchEntire(s) != null){
                    hasBennMatched = true
                } else if (hasBennMatched) {
                    break
                } else {
                    throw ParseException(saved.location(source), saved.location(source).range, "expected $regex found ${saved.location(source).lexeme}")
                }
                s += src.advance(1)
                i += 1
            }
            source.advance(i)
            saved.location(source)
        }, "regex $regex")
    }

    /**
     * Attempts to parse the input source using a series of parsing functions and returns the result of the first successful parse.
     *
     * @param f A variable number of parsing functions (ParseFn<T>) to be attempted in order.
     * @return A [ParseFn] that returns the result of the first successful parse.
     * @throws ParseException if none of the provided parsing functions match the input source.
     */
    fun<T> choices(vararg f: ParseFn<T>): ParseFn<T> {
        return ParseFn({
            var res: T? = null
            var hasChanged = false
            val saved = source.save()
            for (func in f) {
                try {
                    res = func.invoke(this)
                    hasChanged = true
                } catch (x: ParseException) {
                    this.source = saved
                    continue
                }
                break
            }
            if (!hasChanged) {
                val names = f.joinToString(", ") { it.name ?: "<unknown>" }
                throw ParseException(saved.location(source), saved.location(source).range, "no match: expected one of $names")
            }

            res!!
        }, f.joinToString(", ") { it.name ?: "<unknown>" })
    }

    /**
     * Parses the input source multiple times using a given parsing function, collecting the results in a list.
     *
     * @param f The parsing function (ParseFn<T>) to be applied repeatedly.
     * @param min The minimum number of times the parsing function must successfully match. Defaults to 0.
     * @param max The maximum number of times the parsing function can successfully match. Defaults to -1, indicating no upper limit.
     * @return A [ParseFn] that returns a list of all successful parse results.
     * @throws ParseException if the number of successful parses is less than min or greater than max (when max is not -1).
     */
    fun<T> many(f: ParseFn<T>, min: Int = 0, max: Int = -1, ): ParseFn<List<T>> {
        val obj = this
        return ParseFn({
            val saved = source.save()
            val x = mutableListOf<T>()
            var i = 0

            while (true) {
                val oldSource = source.save()
                try {
                    x.add(f(obj))
                } catch (x: ParseException) {
                    source = oldSource
                    break
                }
                i += 1
            }

            if (i < min || (i > max && max != -1)) {
                throw ParseException(saved.location(source), saved.location(source).range, "expected $min..$max found $i")
            }
            x
        }, "many of (${f.name})")
    }

    /**
     * Defines a recursive parsing function using a provided factory function.
     *
     * **Warning:** Be cautious when using recursive parsers to avoid infinite recursion. Ensure that your recursive parser has a well-defined base case or termination condition. Infinite recursion can lead to stack overflow errors and unintended behavior.
     *
     * @param f A factory function that takes a [ParseFn<T>] (the recursive parser placeholder) and returns a [ParseFn<T>] (the actual recursive parser).
     * @return A [ParseFn<T>] that represents the recursive parsing function.
     */
    fun<T> rec(f: (ParseFn<T>) -> ParseFn<T>): ParseFn<T> {
        var recursiveParser: ParseFn<T>? = null

        val lazyParser: ParseFn<T> = ParseFn({
            recursiveParser!!.invoke(this)
        })

        recursiveParser = f(lazyParser)
        return ParseFn({ recursiveParser.invoke(this) })
    }

    /**
     * Sequentially applies a series of parsing functions to the input source and collects the results in a list.
     *
     * @param f A variable number of parsing functions (ParseFn<T>) to be applied in sequence.
     * @return A [ParseFn] that returns a list of results from applying each parsing function in sequence
     *
     * @throws ParseException if any of the parsing functions encounter an error while parsing.
     *
     */
    fun<T> seq(vararg f: ParseFn<T>): ParseFn<List<T>> {
        return ParseFn({
            f.map { it.invoke(this) }.filterIndexed { index, _ -> !f[index].canBeIgnored }
        })
    }

    /**
     * Adds specified characters as whitespace to the input source.
     *
     * @param w A variable number of characters (Char) to be treated as whitespace.
     */
    fun whitespace(vararg w: Char) {
        w.forEach { source.addWhitespace(it) }
    }

}

/**
 * Interface representing a parser that processes a string input and returns a result of type [T].
 *
 * The Parse interface defines methods for parsing strings and handling errors. It also includes utility methods for transforming results
 * and errors, and for obtaining parsing results along with their locations.
 *
 * @param T The type of the result produced by the parser.
 */
interface Parse<T> {
    /**
     * Parses the given string and returns the result.
     *
     * @param s The input string to be parsed.
     * @return The parsed result of type [T].
     * @throws ParseException if parsing fails.
     */
    fun parse(s: String): T

    /**
     * Parses the given string and returns the result or null if parsing fails.
     *
     * @param s The input string to be parsed.
     * @return The parsed result of type [T] or null if parsing fails.
     */
    fun parseOrNull(s: String): T? {
        return try {
            parse(s)
        } catch (x: ParseException) {
            null
        }
    }

    /**
     * Parses the given string and returns the result along with the location information.
     *
     * @param s The input string to be parsed.
     * @return A pair where the first element is the parsed result of type [T] and the second element is the location of the parse attempt.
     */
    fun parseWithLocation(s: String): Pair<T?, Location> {
        return try {
            val r = parse(s)
            Pair(r, Location(0..s.length, s, Source(s)))
        } catch (x: ParseException) {
            Pair(null, x.loc)
        }
    }

    /**
     * Transforms the result of the parser using the given function.
     *
     * @param f A function that takes a result of type [T] and returns a transformed result of type [T].
     * @return A new [Parse] instance that applies the transformation function to the result of the original parser.
     */
    fun map(f: (T) -> T): Parse<T> {
        val obj = this
        return object: Parse<T> {
            override fun parse(s: String): T {
                return f(obj.parse(s))
            }
        }
    }

    /**
     * Maps the ParseException thrown by the parser to a different exception type.
     *
     * @param f A function that takes a ParseException and returns a different Exception.
     * @return A new [Parse] instance that throws the exception produced by the mapping function if parsing fails.
     */
    fun<E: Exception> mapError(f: (ParseException) -> Exception): Parse<T> {
        val obj = this
        return object: Parse<T> {
            override fun parse(s: String): T {
                return try {
                    obj.parse(s)
                } catch (x: ParseException) {
                    throw f(x)
                }
            }
        }
    }

    /**
     * Parses the given string and returns the result along with any ParseException encountered.
     *
     * @param s The input string to be parsed.
     * @return A pair where the first element is the parsed result of type [T] or null if parsing fails,
     *         and the second element is the ParseException encountered or null if parsing succeeds.
     */
    fun parseAsPair(s: String): Pair<T?, ParseException?> {
        return try {
            Pair(parse(s), null)
        } catch (x: ParseException) {
            Pair(null, x)
        }
    }
}

/**
 * Creates a parser using the provided parsing rule and the [Parser] context.
 *
 * The syntax function allows the definition of a parser based on a rule provided as a lambda function.
 * The lambda function receives a [Parser] instance and returns a [ParseFn] that performs the actual parsing.
 *
 * @param rule A lambda function that defines the parsing rule. It receives a [Parser] instance and returns a [ParseFn<T>].
 * @return A [Parse] instance that represents the parser created using the provided rule.
 *
 */
fun <T> syntax(rule: Parser.() -> ParseFn<T>): Parse<T> = object: Parse<T> {
    override fun parse(s: String): T {
        val parser = Parser(Source(s))
        val r = rule(parser)
        val res = r(parser)
        return res
    }
}