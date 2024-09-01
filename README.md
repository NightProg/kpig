# kpig
A wonderful parser generator written in kotlin
```kotlin
import io.nightprog.kpig.syntax

fun main() {
    val syntax = syntax {
        whitespace(' ', '\t', '\n', '\r')
        val number = regex("\\d+") map { it.lexeme.toInt() }
        val termOp = tag("+") or tag("-") map { it.lexeme }
        val factorOp = tag("*") or tag("/") map { it.lexeme }
        val factor = rec<Int> {
            number then many(factorOp then it, min = 1) map { (lhs, list) ->
                list.fold(lhs) { acc, (op, rhs) ->
                    when (op) {
                        "*" -> acc * rhs
                        "/" -> acc / rhs
                        else -> error("Invalid operator")
                    }
                }
            } or number
        }

        val expr = rec {
            number then many(termOp then factor, min = 1) map { (lhs, list) ->
                list.fold(lhs) { acc, (op, rhs) ->
                    when (op) {
                        "+" -> acc + rhs
                        "-" -> acc - rhs
                        else -> error("Invalid operator")
                    }
                }
            } or factor
        }

        expr
    }

    val res = syntax.parse("91 + 3 * 4 / 2 - 72")
    assert(res == 25)
}
```
