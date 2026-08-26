![CodeDNA](banner.png)

# CodeDNA

**English** · [Português](README.pt-BR.md)

**Which parts of a program are actually load-bearing?**

CodeDNA answers that with an experiment borrowed from genetics: knock out one gene, watch what the
organism does, put it back, repeat. Here the organism is a Java program and the genes are its methods.

```
source code -> run -> remove one method -> run again -> compare -> essentiality
```

The whole analyser is one file, ~120 lines, zero dependencies.

## The problem

Coverage tells you which lines *ran*. Complexity metrics tell you which lines are *hard to read*.
Neither tells you what you actually want to know when you open an unfamiliar codebase:

> If this method disappeared tomorrow, would anyone notice?

That question is normally answered by deleting things and seeing what breaks — which nobody does,
because it is tedious and destructive. CodeDNA does exactly that, automatically, on a copy.

## The concept

Geneticists identify what a gene is for by disabling it and observing the resulting organism.
A *knockout* that kills the organism marks an essential gene. A knockout nothing notices marks a
passenger.

CodeDNA applies the same protocol to code. It empties one method body at a time — keeping the
signature so everything still compiles — recompiles the program, runs it, and measures how much of
the original behaviour survived.

**Essentiality = the fraction of the program's observable behaviour that disappears when the method
is removed.**

No annotations, no test framework, no config file. It reads a `.java` file and reports.

## How it works

1. Run the untouched program and record its output. This is the healthy phenotype.
2. Find every method by brace depth, and for each one produce a variant of the source where that
   body is replaced by `return 0;` / `return null;` / nothing, depending on the return type.
3. Compile and run each variant in an isolated classloader, capturing its output.
4. Compare against the healthy output, line by line. Lines that no longer appear are behaviour lost.
   A variant that crashes or fails to compile scores 100% — the organism is not viable.
5. Draw the result as a bar chart.

That is the entire program. `javax.tools.JavaCompiler` — the compiler that already ships inside every
JDK — does the compiling, so there is no build system and nothing to install.

## Example

```java
public class Demo {
    static int[] parse(String order)              { ... }
    static boolean valid(int[] item)              { ... }
    static int total(int[] item)                  { ... }
    static String format(int[] item, int total)   { ... }
    static void log(String message)               { ... }
    static void debug(String message)             { ... }

    public static void main(String[] args)        { ... }
}
```

```bash
javac -d build src/CodeDNA.java
java -cp build CodeDNA examples/Demo.java
```

## Result

```
CODE DNA

parse()   ██████████ 100%
valid()   ███████     67%
total()   ███████     67%
format()  ██████      56%
log()     ██          22%
debug()   █           11%
```

Read from the top: without `parse()` the program does not survive at all. `valid()` and `total()`
carry the same weight — remove either one and two thirds of the output changes. `format()` only
affects how results are printed, not whether they are right. `log()` and `debug()` are passengers:
the program computes exactly the same answers without them.

Nobody wrote a test, an assertion or an annotation to produce this. It came out of the source file
alone.

[`notebook/CodeDNA.ipynb`](notebook/CodeDNA.ipynb) runs the same analysis and plots it.

## Requirements

A JDK 17 or newer — that is all. The notebook additionally uses `matplotlib` for the chart.

## Current limitations

These are honest boundaries of the MVP, not a roadmap in disguise:

- **One file, one entry point.** The subject must be a single `.java` file with a `main` method.
- **Behaviour means stdout.** Return values, files and side effects are invisible to the oracle.
  A method that only mutates state and prints nothing scores 0%.
- **A chatty method looks important.** Essentiality is measured against printed output, so a logger
  that prints half the lines is scored as half the behaviour. Judging against assertions instead of
  output would fix this.
- **Coverage bounds the verdict.** A method that this particular run never reaches scores 0%,
  whether or not it matters elsewhere.
- **The parser is a brace counter.** Braces inside string literals or comments confuse it.
- **Mutants run in-process.** An infinite loop hangs the analysis and `System.exit` ends it.

## Possible future directions

- **Use a test suite as the oracle** instead of stdout — essentiality becomes "percentage of tests
  that fail", which removes the chatty-logger bias.
- **Double knockouts.** Genetics calls it epistasis: does `A` only matter when `B` is missing?
  Pairwise knockouts would expose redundant code paths and silent fallbacks.
- **Other languages.** Nothing here is Java-specific except the compiler call. Any language with a
  "compile" and a "run" step fits the same protocol.
- **DNA as an artifact.** Emit the profile as JSON and diff it across commits, to see essentiality
  shift as a codebase evolves.

## License

MIT
