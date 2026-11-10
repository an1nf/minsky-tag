package minsky;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import minsky.cm.CounterMachine;
import minsky.tag.TagSystem;
import minsky.tm.Run;
import minsky.tm.TuringMachine;
import minsky.translate.CountersToTwo;
import minsky.translate.Sym;
import minsky.translate.TmToCounters;
import minsky.translate.TmToTag;

/**
 * Ejecuta los ejemplos por toda la cadena, verifica las trazas y escribe en el
 * directorio de salida los programas compilados, las producciones y una traza.
 *
 * Uso: java minsky.Main [directorio-salida] [--trace]
 */
public final class Main {

    private static final int LIMIT = 10_000;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        boolean trace = Arrays.asList(args).contains("--trace");
        Path out = Path.of(Arrays.stream(args).filter(a -> !a.startsWith("--")).findFirst().orElse("output"));
        Files.createDirectories(out);

        String[] header = {"MT", "entrada", "cinta final", "pasos MT", "instr. 3C", "pasos 3C",
            "instr. 2C", "pasos 2C", "Z máx", "prod. 2-tag", "pasos 2-tag", "palabra máx", "pasos cíclico"};
        List<String[]> rows = new ArrayList<>();

        for (Examples.Example ex : Examples.all()) {
            TuringMachine tm = ex.tm();
            int[] in = ex.input();
            Run ref = tm.run(in, LIMIT);

            Pipeline.Report r3 = Pipeline.runCounters(tm, in, LIMIT, true);
            Pipeline.check(tm, in, r3, LIMIT);
            Pipeline.Report r2 = Pipeline.runTwoCounters(tm, in, LIMIT, true);
            Pipeline.check(tm, in, r2, LIMIT);

            String[] tag = {"-", "-", "-", "-"};
            if (tm.base() == 2) {
                Pipeline.Report rt = Pipeline.runTag(tm, in, LIMIT);
                Pipeline.check(tm, in, rt, LIMIT);
                TagSystem<Sym> ts = TmToTag.compile(tm);
                long cyc = Pipeline.cyclicCheck(ts, TmToTag.encode(tm.initial(in)), rt.steps().longValue());
                tag = new String[] {String.valueOf(rt.size()), rt.steps().toString(),
                    String.valueOf(rt.maxLength()), String.valueOf(cyc)};
                Files.writeString(out.resolve(tm.name() + "_2tag.txt"), ts.listing());
            }

            StringJoiner inStr = new StringJoiner("");
            Arrays.stream(in).forEach(s -> inStr.add(String.valueOf(s)));
            rows.add(new String[] {tm.name(), in.length == 0 ? "ε" : inStr.toString(),
                ref.last().tape(tm.base()), String.valueOf(ref.steps()),
                String.valueOf(r3.size()), r3.steps().toString(),
                String.valueOf(r2.size()), sci(r2.steps()), sci(r2.zMax()),
                tag[0], tag[1], tag[2], tag[3]});

            CounterMachine cm3 = TmToCounters.compile(tm);
            Files.writeString(out.resolve(tm.name() + "_3contadores.txt"), cm3.listing());
            Files.writeString(out.resolve(tm.name() + "_2contadores.txt"),
                CountersToTwo.compile(cm3).listing());
        }

        String table = table(header, rows);
        Files.writeString(out.resolve("resultados.md"),
            "# Resultados\n\nTodas las trazas coinciden paso a paso con la MT de referencia.\n\n" + table);
        System.out.print(table);
        System.out.println();
        System.out.println("Trazas verificadas. Ficheros en " + out.toAbsolutePath());

        List<String> log = new ArrayList<>();
        TuringMachine bb2 = Examples.busyBeaver2();
        TmToTag.compile(bb2).run(TmToTag.encode(bb2.initial(new int[] {})), 120, (w, s) -> {
            StringJoiner j = new StringJoiner(" ");
            w.stream().limit(16).forEach(x -> j.add(x.toString()));
            log.add(String.format("%4d  %s%s", s, j, w.size() > 16 ? " ..." : ""));
            return true;
        });
        Files.write(out.resolve("castor-2_traza_2tag.txt"), log);
        if (trace) {
            log.stream().limit(60).forEach(System.out::println);
        }
    }

    private static String sci(BigInteger n) {
        String s = n.toString();
        return s.length() <= 7 ? s : s.charAt(0) + "." + s.substring(1, 3) + "e" + (s.length() - 1);
    }

    private static String table(String[] header, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("| ").append(String.join(" | ", header)).append(" |\n|");
        for (int i = 0; i < header.length; i++) {
            sb.append("---|");
        }
        sb.append('\n');
        for (String[] r : rows) {
            sb.append("| ").append(String.join(" | ", r)).append(" |\n");
        }
        return sb.toString();
    }
}
