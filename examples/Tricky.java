/** Methods that used to fool the analyser: braces inside literals and comments, and throws clauses. */
public class Tricky {

    static String brace(String s) {
        // a comment with { an unbalanced brace
        /* and another } here */
        char open = '{';
        return open + s + "{";
    }

    static int risky(String s) throws Exception {
        if (s.isEmpty()) throw new Exception("empty");
        return s.length();
    }

    static String block(String s) throws java.io.IOException, IllegalStateException {
        return """
            {
            """ + s;
    }

    static void report(String s) {
        System.out.println("report " + s);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(brace("a"));
        System.out.println(risky("abc"));
        System.out.println(block("x"));
        report("done");
    }
}
