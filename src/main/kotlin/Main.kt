
fun main() {
    val parser = syntax {
        whitespace('\n', ' ', '\t', '\r')
        val number = regex("[0-9]+") map { it.lexeme.toInt() }
        val termOp = tag("+") or tag("-") map { it.lexeme }
        val factorOp = tag("*") or tag("/") map { it.lexeme }
        val factor = number then factorOp then number map {
            val (first, rhs) = it
            val (lhs, op) = first
            if (op == "*") {
                lhs * rhs
            } else {
                lhs / rhs
            }
        }
        val term = number then termOp then factor map {
            val (first, rhs) = it
            val (lhs, op) = first
            if (op == "+") {
                lhs + rhs
            } else {
                lhs - rhs
            }
        }


        term
    }

    println("10 + 22 - 9 = ${parser.parse("10 + 22 * 9")}")
}

/*


syntax {

    rule("ident") {
        regex("[a-zA-Z_][a-zA-Z0-9_]*", (s: String) {
            if (s == "if") {
                return "if_keyword"
            }
            return s
        })
    }

    rule("number") {
        regex("[0-9]+")
    }

    rule("string") {
        regex("\"[^\"]*\"")
    }

    rule("comment") {
        regex("//.*")
    }

    rule("if_keyword") {
        simple("if")
    }

    rule("if_statement") {
        tag("if_keyword")
        tag("ident")
        tag("number")
    }


 */