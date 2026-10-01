import java.util.List;

// A generic method declares a type parameter before its return type:
//     static <T> T pick(...)
// The signature parser used to read past the modifiers straight into a return
// type, so the "<T>" prefix hid the method and it never got a gene. This file
// pins that: pick() and first() both declare type parameters and must appear.
public class Generic {

    static <T> T pick(List<T> xs, int i) {
        return xs.get(i);
    }

    static <T> T first(List<T> xs) {
        return pick(xs, 0);
    }

    static int size(List<String> xs) {
        return xs.size();
    }

    public static void main(String[] args) {
        List<String> xs = List.of("a", "b", "c");
        System.out.println(pick(xs, 1));
        System.out.println(first(xs));
        System.out.println(size(xs));
    }
}
