
class Parser<T>(val source: Source, var rules: MutableMap<String, Parser<T>.() -> T?> = mutableMapOf()) {
    companion object
}

interface Parse<T> {
    fun parse(s: Source): T?;
}

fun<T> syntax(rule: Parser<T>.() -> T?) = object: Parse<T>{
    override fun parse(s: Source): T? {
        return rule(Parser(s))
    }
}

fun<T> Parser<T>.rule(name: String, rule: Parser<T>.() -> T?) {
    this.rules[name] = rule;
}

fun<T> Parser<T>.tag(name: String): T? {
    val x = this.rules[name] ?: throw RuntimeException("no rule found")
    return x.invoke(this)
}

fun<T> Parser<T>.simple(v: String, f: (Location) -> T? = {null}): T?{
    val saved = source.save();
    for (c in v) {
        if (source.eof()) {
            throw RuntimeException("eof")
        }
        if (source.peek() != c) {
            throw RuntimeException("rule not respected")
        }
        source.advance(1);
    }

    return f(saved.location(source));
}