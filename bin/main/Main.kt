

fun main(args: Array<String>) {
    val x = syntax<Int> {
        rule("if") {
            simple("if") {
                1000
            }
        }
        rule("else") {
            simple("else")
            simple("then") {
                4000
            }
        }

        tag("if")!! + tag("else")!!
    }

    println(x.parse(Source("ifelsethen")));
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