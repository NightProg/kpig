import java.util.regex.Pattern

class ParseException(loc: Location, problem: IntRange, message: String): Exception(formatError(loc, problem, message)) {
    companion object {
        fun formatError(loc: Location, problem: IntRange, message: String): String {
            var msg = "Error at ${loc.source.line()}:${loc.source.column()}: $message"
            msg += "\n${loc.getCurrentLine()}"
            msg += "\n" + " ".repeat(problem.first) + "^".repeat(problem.last - problem.first)
            return msg
        }
    }
}

typealias ParseFunc<T> = Parser.() -> T

class ParseFn<T>(val f: ParseFunc<T>, val name: String? = null) {
    operator fun invoke(parser: Parser): T {
        return f(parser)
    }
}

class Parser(var source: Source) {
    fun tag(v: String): ParseFunc<Location> {
        return {
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
        }
    }

    fun regex(regex: String): ParseFunc<Location> {
        return {
            val pattern = Pattern.compile(regex).toRegex()
            val src = source.copy()
            val saved = source.save()
            var i = 0
            var s = src.peek().toString()
            var hasBennMatched = false;
            while (true) {
                if (src.eof()) {
                    if (hasBennMatched) {
                        break
                    }
                    throw RuntimeException("regex not found")
                }

                if (pattern.matchEntire(s) != null){
                    hasBennMatched = true
                } else if (hasBennMatched) {
                    break
                } else {
                    throw RuntimeException("regex not found")
                }
                s += src.advance(1)
                i += 1
            }
            source.advance(i)
            saved.location(source)
        }
    }

    fun<T> choices(vararg f: ParseFunc<T>): ParseFunc<T> {
        return {
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
                throw ParseException(saved.location(source), saved.location(source).range, "no match")
            }

            res!!
        }
    }

    fun<T> many(f: ParseFunc<T>, min: Int = 0, max: Int = -1, ): ParseFunc<List<T>> {
        val obj = this
        return {
            val saved = source.save()
            val x = mutableListOf<T>()
            var i = 0

            while (true) {
                val oldSource = source.save()
                try {
                    x.add(f(obj))
                } catch (x: RuntimeException) {
                    source = oldSource
                    break
                }
                i += 1
            }

            if (i < min || (i > max && max != -1)) {
                throw ParseException(saved.location(source), saved.location(source).range, "expected $min..$max found $i")
            }
            x
        }
    }


    fun<T> rec(f: (ParseFunc<T>) -> ParseFunc<T>): ParseFunc<T> {
        var recursiveParser: ParseFunc<T>? = null

        val lazyParser: ParseFunc<T> = {
            recursiveParser!!.invoke(this)
        }

        recursiveParser = f(lazyParser)
        return { recursiveParser!!.invoke(this) }
    }

    fun seq(vararg f: ParseFunc<Any>): ParseFunc<List<Any>> {
        return {
            f.map { it.invoke(this) }.filter { it !is Unit }
        }
    }


    fun whitespace(vararg w: Char) {
        w.forEach { source.addWhitespace(it) }
    }

}

interface Parse<T> {
    fun parse(s: String): T
}

fun<T> syntax(rule: Parser.() -> ParseFunc<T>) = object: Parse<T> {
    override fun parse(s: String): T {
        val parser = Parser(Source(s))
        val r = rule(parser)
        val res = r(parser)
        return res
    }
}


infix fun<T, R> ParseFunc<T>.map(f: (T) -> R): ParseFunc<R> {
    val obj = this
    return {
        f(obj.invoke(this))
    }
}

infix fun<T, R> ParseFunc<T>.then(f: ParseFunc<R>): ParseFunc<Pair<T, R>> {
    val obj = this
    return {
        Pair(obj.invoke(this), f.invoke(this))
    }
}

infix fun<T, R> ParseFunc<T>.thenIgnore(f: ParseFunc<R>): ParseFunc<T> {
    val obj = this
    return {
        val r = obj.invoke(this)
        f.invoke(this)
        r
    }
}

infix fun<T, R> ParseFunc<T>.rthen(f: ParseFunc<R>): ParseFunc<R> {
    val obj = this
    return {
        obj.invoke(this)
        f.invoke(this)
    }
}

infix fun<T> ParseFunc<T>.or(f: ParseFunc<T>): ParseFunc<T> {
    val obj = this
    return {
        choices(obj, f).invoke(this)
    }
}

fun<T> ParseFunc<T>.ignore(): ParseFunc<Unit> {
    val obj = this
    return {
        obj.invoke(this)
    }
}

