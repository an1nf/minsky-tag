package minsky.cm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ensamblador con etiquetas simbólicas. Un destino null es la instrucción siguiente.
 * Las etiquetas pueden ser cualquier objeto con equals/hashCode (p. ej. List.of("Q", q)).
 */
public final class Assembler {

    private record Pending(Instr.Op op, int reg, Object next, Object zero) {
    }

    private final String[] names;
    private final List<Pending> code = new ArrayList<>();
    private final Map<Object, Integer> labels = new HashMap<>();
    private final Map<Integer, String> checkpoints = new HashMap<>();
    private final Map<Integer, String> halts = new HashMap<>();
    private int counter;

    public Assembler(String... names) {
        this.names = names.clone();
    }

    public String fresh(String hint) {
        return "_" + hint + (++counter);
    }

    public void label(Object name) {
        if (labels.putIfAbsent(name, code.size()) != null) {
            throw new IllegalStateException("etiqueta duplicada: " + name);
        }
    }

    /** Marca la próxima instrucción como punto de control. */
    public void checkpoint(String info) {
        checkpoints.put(code.size(), info);
    }

    public void inc(int r, Object next) {
        code.add(new Pending(Instr.Op.INC, r, next, null));
    }

    public void dec(int r, Object next, Object zero) {
        code.add(new Pending(Instr.Op.DEC, r, next, zero));
    }

    public void halt(String info) {
        halts.put(code.size(), info);
        code.add(new Pending(Instr.Op.HALT, -1, null, null));
    }

    /** dst += k*src; src := 0; ir a then. */
    public void move(int src, int dst, int k, Object then) {
        if (k < 1) {
            throw new IllegalArgumentException("k < 1");
        }
        String loop = fresh("mv");
        label(loop);
        dec(src, null, then);
        for (int i = 0; i < k; i++) {
            inc(dst, i == k - 1 ? loop : null);
        }
    }

    /** r += c; ir a then. */
    public void addConst(int r, int c, Object then) {
        if (c < 1) {
            throw new IllegalArgumentException("c < 1");
        }
        for (int i = 0; i < c; i++) {
            inc(r, i == c - 1 ? then : null);
        }
    }

    public CounterMachine assemble(String name) {
        Instr[] out = new Instr[code.size()];
        for (int i = 0; i < out.length; i++) {
            Pending p = code.get(i);
            out[i] = switch (p.op()) {
                case INC -> Instr.inc(p.reg(), resolve(p.next(), i));
                case DEC -> Instr.dec(p.reg(), resolve(p.next(), i), resolve(p.zero(), i));
                case HALT -> Instr.halt();
            };
        }
        return new CounterMachine(name, out, names, checkpoints, halts);
    }

    private int resolve(Object target, int i) {
        if (target == null) {
            if (i + 1 >= code.size()) {
                throw new IllegalStateException("fall-through fuera del programa en " + i);
            }
            return i + 1;
        }
        Integer pc = labels.get(target);
        if (pc == null) {
            throw new IllegalStateException("etiqueta no definida: " + target);
        }
        return pc;
    }
}
