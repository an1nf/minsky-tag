package minsky.tm;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MT determinista de una cinta sobre {0, ..., b-1}; 0 es el blanco.
 * Para cuando delta(q, s) no está definida.
 *
 * run() simula sobre una cinta explícita, sin usar la codificación numérica:
 * sirve de referencia independiente para verificar las traducciones.
 */
public final class TuringMachine {

    private final String name;
    private final String start;
    private final int base;
    private final Map<String, Transition[]> delta = new LinkedHashMap<>();
    private final Set<String> states = new LinkedHashSet<>();

    public TuringMachine(String name, String start, int base) {
        if (base < 2) {
            throw new IllegalArgumentException("base < 2");
        }
        this.name = name;
        this.start = start;
        this.base = base;
        states.add(start);
    }

    public TuringMachine(String name, String start) {
        this(name, start, 2);
    }

    public TuringMachine on(String q, int s, String next, int write, char move) {
        if (s < 0 || s >= base || write < 0 || write >= base) {
            throw new IllegalArgumentException("símbolo fuera del alfabeto en (" + q + ", " + s + ")");
        }
        if (move != 'L' && move != 'R') {
            throw new IllegalArgumentException("movimiento inválido: " + move);
        }
        states.add(q);
        states.add(next);
        delta.computeIfAbsent(q, k -> new Transition[base])[s] = new Transition(next, write, move);
        return this;
    }

    public Transition transition(String q, int s) {
        Transition[] row = delta.get(q);
        return row == null ? null : row[s];
    }

    public int transitionCount() {
        int n = 0;
        for (Transition[] row : delta.values()) {
            for (Transition t : row) {
                if (t != null) {
                    n++;
                }
            }
        }
        return n;
    }

    public String name() {
        return name;
    }

    public String start() {
        return start;
    }

    public int base() {
        return base;
    }

    public List<String> states() {
        return List.copyOf(states);
    }

    public Run run(int[] input, int maxSteps) {
        Map<Integer, Integer> tape = new HashMap<>();
        for (int i = 0; i < input.length; i++) {
            if (input[i] != 0) {
                tape.put(i, input[i]);
            }
        }
        int head = 0;
        String q = start;
        List<Config> trace = new ArrayList<>();
        trace.add(config(q, tape, head));
        for (int t = 0; t < maxSteps; t++) {
            Transition tr = transition(q, tape.getOrDefault(head, 0));
            if (tr == null) {
                break;
            }
            if (tr.write() != 0) {
                tape.put(head, tr.write());
            } else {
                tape.remove(head);
            }
            head += tr.move() == 'R' ? 1 : -1;
            q = tr.next();
            trace.add(config(q, tape, head));
        }
        boolean halted = transition(q, tape.getOrDefault(head, 0)) == null;
        return new Run(trace, halted, Map.copyOf(tape), head);
    }

    public Config initial(int[] input) {
        return new Config(start, BigInteger.ZERO, encode(input, base));
    }

    /**
     * Paso aritmético sobre (q, L, R); null si no hay transición.
     * Derecha:   L' = L*b + w,   R' = R div b.
     * Izquierda: L' = L div b,   R' = ((R div b)*b + w)*b + L mod b.
     */
    public Config step(Config c) {
        BigInteger b = BigInteger.valueOf(base);
        BigInteger[] qr = c.right().divideAndRemainder(b);
        Transition tr = transition(c.state(), qr[1].intValue());
        if (tr == null) {
            return null;
        }
        BigInteger w = BigInteger.valueOf(tr.write());
        if (tr.move() == 'R') {
            return new Config(tr.next(), c.left().multiply(b).add(w), qr[0]);
        }
        BigInteger[] lq = c.left().divideAndRemainder(b);
        BigInteger r = qr[0].multiply(b).add(w).multiply(b).add(lq[1]);
        return new Config(tr.next(), lq[0], r);
    }

    private Config config(String q, Map<Integer, Integer> tape, int head) {
        BigInteger b = BigInteger.valueOf(base);
        BigInteger l = BigInteger.ZERO;
        BigInteger r = BigInteger.ZERO;
        if (!tape.isEmpty()) {
            int lo = Collections.min(tape.keySet());
            int hi = Collections.max(tape.keySet());
            for (int p = hi; p >= head; p--) {
                r = r.multiply(b).add(BigInteger.valueOf(tape.getOrDefault(p, 0)));
            }
            for (int p = lo; p < head; p++) {
                l = l.multiply(b).add(BigInteger.valueOf(tape.getOrDefault(p, 0)));
            }
        }
        return new Config(q, l, r);
    }

    /** cells[0] es el dígito menos significativo. */
    public static BigInteger encode(int[] cells, int base) {
        BigInteger b = BigInteger.valueOf(base);
        BigInteger n = BigInteger.ZERO;
        for (int i = cells.length - 1; i >= 0; i--) {
            n = n.multiply(b).add(BigInteger.valueOf(cells[i]));
        }
        return n;
    }

    public static List<Integer> digits(BigInteger n, int base) {
        BigInteger b = BigInteger.valueOf(base);
        List<Integer> out = new ArrayList<>();
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(b);
            out.add(qr[1].intValue());
            n = qr[0];
        }
        return out;
    }

    @Override
    public String toString() {
        return name;
    }
}
