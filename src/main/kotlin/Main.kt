
fun main() {
    val parser = syntax {
        whitespace('\n', ' ', '\t', '\r')
        choices(
            tag("+"),
            tag("-"),
        ) or regex("[0-9]+").named("number")
    }

    println("10 + 22 - 9 = ${parser.parse("k")}")
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