package minsky.cm;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Máquina de Minsky ensamblada y su simulador. */
public final class CounterMachine {

    @FunctionalInterface
    public interface Observer {
        /** Se invoca en cada punto de control; false detiene la simulación. */
        boolean visit(String info, BigInteger[] regs, BigInteger steps);
    }

    private final String name;
    private final Instr[] code;
    private final String[] names;
    private final Map<Integer, String> checkpoints;
    private final Map<Integer, String> halts;

    CounterMachine(String name, Instr[] code, String[] names,
                   Map<Integer, String> checkpoints, Map<Integer, String> halts) {
        this.name = name;
        this.code = code;
        this.names = names;
        this.checkpoints = Collections.unmodifiableMap(new HashMap<>(checkpoints));
        this.halts = Collections.unmodifiableMap(new HashMap<>(halts));
    }

    /** Construye una máquina directamente (sin etiquetas ni puntos de control). */
    public static CounterMachine of(String name, int registers, Instr... code) {
        String[] names = new String[registers];
        for (int i = 0; i < registers; i++) {
            names[i] = "c" + i;
        }
        return new CounterMachine(name, code.clone(), names, Map.of(), Map.of());
    }

    public String name() {
        return name;
    }

    public int size() {
        return code.length;
    }

    public int registers() {
        return names.length;
    }

    public Instr instr(int pc) {
        return code[pc];
    }

    public String checkpointAt(int pc) {
        return checkpoints.get(pc);
    }

    public String haltInfoAt(int pc) {
        return halts.get(pc);
    }

    public Result run(long... regs) {
        BigInteger[] r = new BigInteger[regs.length];
        for (int i = 0; i < regs.length; i++) {
            r[i] = BigInteger.valueOf(regs[i]);
        }
        return run(r, null, null);
    }

    public Result run(BigInteger[] init, BigInteger maxSteps, Observer obs) {
        if (init.length != names.length) {
            throw new IllegalArgumentException("número de contadores incorrecto");
        }
        BigInteger[] r = init.clone();
        BigInteger steps = BigInteger.ZERO;
        int pc = 0;
        while (true) {
            String cp = checkpoints.get(pc);
            if (obs != null && cp != null && !obs.visit(cp, r.clone(), steps)) {
                return new Result(r, steps, false, pc, null, true);
            }
            Instr ins = code[pc];
            if (ins.op() == Instr.Op.HALT) {
                return new Result(r, steps, true, pc, halts.get(pc), false);
            }
            if (maxSteps != null && steps.compareTo(maxSteps) >= 0) {
                return new Result(r, steps, false, pc, null, false);
            }
            if (ins.op() == Instr.Op.INC) {
                r[ins.reg()] = r[ins.reg()].add(BigInteger.ONE);
                pc = ins.next();
            } else if (r[ins.reg()].signum() > 0) {
                r[ins.reg()] = r[ins.reg()].subtract(BigInteger.ONE);
                pc = ins.next();
            } else {
                pc = ins.zero();
            }
            steps = steps.add(BigInteger.ONE);
        }
    }

    public String listing() {
        StringBuilder sb = new StringBuilder();
        sb.append("; ").append(name).append(": ").append(code.length)
          .append(" instrucciones, contadores ").append(String.join(", ", names)).append('\n');
        for (int i = 0; i < code.length; i++) {
            Instr ins = code[i];
            String s = switch (ins.op()) {
                case INC -> "INC " + names[ins.reg()] + ", " + ins.next();
                case DEC -> "DEC " + names[ins.reg()] + ", " + ins.next() + ", " + ins.zero();
                case HALT -> "HALT" + (halts.get(i) != null ? "   ; " + halts.get(i) : "");
            };
            sb.append(String.format("%6d: %s", i, s));
            if (checkpoints.containsKey(i)) {
                sb.append("   ; punto de control ").append(checkpoints.get(i));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
