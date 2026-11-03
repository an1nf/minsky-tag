package minsky.cm;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de Minsky ensamblada y su simulador.
 *
 * Con accelerate = true, los ciclos simples (desde pc, siguiendo siempre la rama
 * "no cero", se vuelve a pc) se ejecutan N vueltas de golpe, donde N es el número
 * de vueltas en que todos los DEC encuentran el contador positivo. El estado final
 * y el contador de pasos son idénticos a los de la ejecución instrucción a instrucción.
 */
public final class CounterMachine {

    @FunctionalInterface
    public interface Observer {
        /** Se invoca en cada punto de control; false detiene la simulación. */
        boolean visit(String info, BigInteger[] regs, BigInteger steps);
    }

    private record Cycle(int length, long[] delta, long[] pmin) {
    }

    private final String name;
    private final Instr[] code;
    private final String[] names;
    private final Map<Integer, String> checkpoints;
    private final Map<Integer, String> halts;
    private final Cycle[] cycles;
    private final boolean[] analysed;

    CounterMachine(String name, Instr[] code, String[] names,
                   Map<Integer, String> checkpoints, Map<Integer, String> halts) {
        this.name = name;
        this.code = code;
        this.names = names;
        this.checkpoints = Collections.unmodifiableMap(new HashMap<>(checkpoints));
        this.halts = Collections.unmodifiableMap(new HashMap<>(halts));
        this.cycles = new Cycle[code.length];
        this.analysed = new boolean[code.length];
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
        return run(r, null, true, null);
    }

    public Result run(BigInteger[] init, BigInteger maxSteps, boolean accelerate, Observer obs) {
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
            if (accelerate) {
                Cycle c = cycle(pc);
                if (c != null) {
                    BigInteger len = BigInteger.valueOf(c.length());
                    BigInteger k = safeIterations(r, c);
                    if (k == null) {
                        if (maxSteps == null) {
                            throw new NonTerminationException(pc);
                        }
                        k = maxSteps.subtract(steps).divide(len);
                    } else if (maxSteps != null) {
                        k = k.min(maxSteps.subtract(steps).divide(len));
                    }
                    if (k.signum() > 0) {
                        for (int i = 0; i < r.length; i++) {
                            if (c.delta()[i] != 0) {
                                r[i] = r[i].add(k.multiply(BigInteger.valueOf(c.delta()[i])));
                            }
                        }
                        steps = steps.add(k.multiply(len));
                        continue;
                    }
                }
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

    /** Ciclo por ramas "no cero" desde pc que no atraviesa puntos de control, o null. */
    private Cycle cycle(int pc) {
        if (analysed[pc]) {
            return cycles[pc];
        }
        analysed[pc] = true;
        if (checkpoints.containsKey(pc)) {
            return null;
        }
        int n = names.length;
        long[] delta = new long[n];
        long[] pmin = new long[n];
        Set<Integer> seen = new HashSet<>();
        seen.add(pc);
        int p = pc;
        int len = 0;
        for (int it = 0; it <= code.length; it++) {
            Instr ins = code[p];
            if (ins.op() == Instr.Op.HALT) {
                return null;
            }
            if (ins.op() == Instr.Op.INC) {
                delta[ins.reg()]++;
            } else {
                delta[ins.reg()]--;
                pmin[ins.reg()] = Math.min(pmin[ins.reg()], delta[ins.reg()]);
            }
            p = ins.next();
            len++;
            if (p == pc) {
                cycles[pc] = new Cycle(len, delta, pmin);
                return cycles[pc];
            }
            if (!seen.add(p) || checkpoints.containsKey(p)) {
                return null;
            }
        }
        return null;
    }

    /**
     * Vueltas completas seguras: max k tal que v + i*delta + pmin >= 0 para todo i < k.
     * null si no hay cota (ciclo infinito).
     */
    private static BigInteger safeIterations(BigInteger[] r, Cycle c) {
        BigInteger best = null;
        for (int i = 0; i < r.length; i++) {
            long m = c.pmin()[i];
            if (m == 0) {
                continue;
            }
            BigInteger slack = r[i].add(BigInteger.valueOf(m));
            if (slack.signum() < 0) {
                return BigInteger.ZERO;
            }
            long d = c.delta()[i];
            if (d < 0) {
                BigInteger k = slack.divide(BigInteger.valueOf(-d)).add(BigInteger.ONE);
                best = best == null ? k : best.min(k);
            }
        }
        return best;
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
