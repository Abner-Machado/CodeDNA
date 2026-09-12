import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knocks out one method at a time and measures how much the program's behaviour changes.
 * The bigger the change, the more essential that method is.
 */
public class CodeDNA {

    static final Pattern SIGNATURE = Pattern.compile(
            "(?:public |private |protected |static |final )+([\\w.$<>\\[\\]]+)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");

    /** A mutant that runs longer than this is treated as dead, so an infinite loop cannot hang the analysis. */
    static final long TIMEOUT_SECONDS = Long.getLong("codedna.timeout", 10);

    /** Where each mutant thread's System.out goes. A thread with no entry prints into the void. */
    static final Map<Thread, OutputStream> SINKS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        Path source = Path.of(args.length > 0 ? args[0] : "examples/Demo.java");
        String code = Files.readString(source);
        String name = source.getFileName().toString().replace(".java", "");
        Path work = Files.createTempDirectory("codedna");
        System.setOut(new PrintStream(new Switchboard(), true, StandardCharsets.UTF_8));

        String healthy = run(code, name, work);
        Map<String, Double> dna = new LinkedHashMap<>();
        for (String[] gene : genes(code)) {
            dna.put(gene[0], divergence(healthy, run(gene[1], name, work)));
        }
        chart(dna);
    }

    /** Every method of the file, paired with the source in which its body was removed. */
    static List<String[]> genes(String code) {
        List<String[]> genes = new ArrayList<>();
        Matcher m = SIGNATURE.matcher(code);
        while (m.find()) {
            if (m.group(2).equals("main")) continue;
            int bodyEnd = bodyEnd(code, m.end() - 1);
            genes.add(new String[]{m.group(2) + "()",
                    code.substring(0, m.end()) + emptyBody(m.group(1)) + code.substring(bodyEnd)});
        }
        return genes;
    }

    /** A body that does nothing but still compiles. */
    static String emptyBody(String returnType) {
        return switch (returnType) {
            case "void" -> "}";
            case "boolean" -> " return false; }";
            case "byte", "short", "int", "long", "float", "double" -> " return 0; }";
            case "char" -> " return (char) 0; }";
            default -> " return null; }";
        };
    }

    /** Compiles this source and captures what its main() prints. Null means the program died or hung. */
    static String run(String code, String name, Path work) throws Exception {
        Path file = work.resolve(name + ".java");
        Files.writeString(file, code);
        if (ToolProvider.getSystemJavaCompiler().run(null, null, OutputStream.nullOutputStream(),
                "-d", work.toString(), file.toString()) != 0) return null;

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{work.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            var main = loader.loadClass(name).getMethod("main", String[].class);
            boolean[] died = {false};
            Thread runner = new Thread(() -> {
                try {
                    main.invoke(null, (Object) new String[0]);
                } catch (ReflectiveOperationException e) {
                    died[0] = true;
                }
            });
            runner.setDaemon(true);
            SINKS.put(runner, captured);
            runner.start();
            runner.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            SINKS.remove(runner);
            if (runner.isAlive() || died[0]) return null;
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    /** How much of the healthy output this run failed to reproduce, from 0.0 to 1.0. */
    static double divergence(String healthy, String mutant) {
        if (mutant == null) return 1.0;
        List<String> expected = healthy.lines().toList();
        List<String> actual = new ArrayList<>(mutant.lines().toList());
        int kept = 0;
        for (String line : expected) if (actual.remove(line)) kept++;
        return expected.isEmpty() ? 0 : 1.0 - (double) kept / expected.size();
    }

    /** Routes System.out by thread: the analyser keeps the console, each mutant gets its own buffer. */
    static class Switchboard extends OutputStream {
        final OutputStream console = new FileOutputStream(FileDescriptor.out);
        final Thread analyser = Thread.currentThread();

        @Override
        public void write(int b) throws java.io.IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            Thread current = Thread.currentThread();
            if (current == analyser) console.write(b, off, len);
            else {
                OutputStream sink = SINKS.get(current);
                if (sink != null) sink.write(b, off, len);
            }
        }
    }

    static int bodyEnd(String code, int openingBrace) {
        int depth = 0;
        for (int i = openingBrace; i < code.length(); i++) {
            if (code.charAt(i) == '{') depth++;
            else if (code.charAt(i) == '}' && --depth == 0) return i + 1;
        }
        return code.length();
    }

    static void chart(Map<String, Double> dna) {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        int width = dna.keySet().stream().mapToInt(String::length).max().orElse(0);
        out.println();
        out.println("CODE DNA");
        out.println();
        dna.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(gene -> out.printf("%-" + width + "s  %-10s %3d%%%n", gene.getKey(),
                        "█".repeat((int) Math.round(gene.getValue() * 10)),
                        Math.round(gene.getValue() * 100)));
    }
}
