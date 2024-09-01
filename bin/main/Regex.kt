import java.util.regex.Pattern


fun<T> Parser<T>.regex(regex: String, f: Parser<T>.() -> T) {
    val pattern = Pattern.compile(regex);

}