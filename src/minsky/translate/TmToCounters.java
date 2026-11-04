package minsky.translate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import minsky.cm.Assembler;
import minsky.cm.CounterMachine;
import minsky.tm.Config;
import minsky.tm.Transition;
import minsky.tm.TuringMachine;

/**
 * MT -> máquina de Minsky de 3 contadores (L, R, T).
 *
 * L y R son las dos pilas de la cinta en base b; T es auxiliar y vale 0 entre macros.
 * Cada estado q tiene un bloque cuyo primer par INC T; DEC T es el punto de control:
 * ahí (L, R, 0) codifica la configuración actual de la MT.
 */
public final class TmToCounters {

    public static final int L = 0;
    public static final int R = 1;
    public static final int T = 2;

    private TmToCounters() {
    }

    public static CounterMachine compile(TuringMachine tm) {
        int b = tm.base();
        Assembler a = new Assembler("L", "R", "T");
        List<String> order = new ArrayList<>();
        order.add(tm.start());
        for (String q : tm.states()) {
            if (!q.equals(tm.start())) {
                order.add(q);
            }
        }
        for (String q : order) {
            a.label(List.of("Q", q));
            a.checkpoint(q);
            a.inc(T, null);
            a.dec(T, List.of("div", q), List.of("div", q));

            // s = R mod b queda en la etiqueta de salida; T = R div b, R = 0
            a.label(List.of("div", q));
            for (int j = 0; j < b; j++) {
                a.dec(R, j == b - 1 ? List.of("dinc", q) : null, List.of("rem", q, j));
            }
            a.label(List.of("dinc", q));
            a.inc(T, List.of("div", q));

            for (int s = 0; s < b; s++) {
                a.label(List.of("rem", q, s));
                String k = a.fresh("r");
                a.move(T, R, 1, k);
                a.label(k);
                emitTransition(a, tm, q, s);
            }
        }
        return a.assemble(tm.name() + " -> 3 contadores");
    }

    private static void emitTransition(Assembler a, TuringMachine tm, String q, int s) {
        int b = tm.base();
        Transition tr = tm.transition(q, s);
        if (tr == null) {
            // restaurar R := R*b + s antes de parar
            String k1 = a.fresh("h");
            String k2 = a.fresh("h");
            mul(a, R, b, k1);
            a.label(k1);
            add(a, R, s, k2);
            a.label(k2);
            a.halt(q);
            return;
        }
        Object target = List.of("Q", tr.next());
        if (tr.move() == 'R') {
            String k1 = a.fresh("t");
            mul(a, L, b, k1);
            a.label(k1);
            add(a, L, tr.write(), target);
            return;
        }
        String k1 = a.fresh("t");
        String k2 = a.fresh("t");
        String k3 = a.fresh("t");
        mul(a, R, b, k1);
        a.label(k1);
        add(a, R, tr.write(), k2);
        a.label(k2);
        mul(a, R, b, k3);
        a.label(k3);
        // t = L mod b, L = L div b, R += t
        String loop = a.fresh("ld");
        String inc = a.fresh("ld");
        String[] rems = new String[b];
        for (int j = 0; j < b; j++) {
            rems[j] = a.fresh("lr");
        }
        a.label(loop);
        for (int j = 0; j < b; j++) {
            a.dec(L, j == b - 1 ? inc : null, rems[j]);
        }
        a.label(inc);
        a.inc(T, loop);
        for (int t = 0; t < b; t++) {
            a.label(rems[t]);
            String k4 = a.fresh("t");
            a.move(T, L, 1, k4);
            a.label(k4);
            add(a, R, t, target);
        }
    }

    /** X := X*b usando T. */
    private static void mul(Assembler a, int x, int b, Object then) {
        String mid = a.fresh("m");
        a.move(x, T, b, mid);
        a.label(mid);
        a.move(T, x, 1, then);
    }

    /** X += c; con c = 0 emite el salto INC T; DEC T. */
    private static void add(Assembler a, int x, int c, Object then) {
        if (c > 0) {
            a.addConst(x, c, then);
        } else {
            a.inc(T, null);
            a.dec(T, then, then);
        }
    }

    public static BigInteger[] registers(Config c) {
        return new BigInteger[] {c.left(), c.right(), BigInteger.ZERO};
    }

    public static Config decode(String state, BigInteger[] regs) {
        if (regs[T].signum() != 0) {
            throw new IllegalStateException("T = " + regs[T] + " en un punto de control");
        }
        return new Config(state, regs[L], regs[R]);
    }
}
