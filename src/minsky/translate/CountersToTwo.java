package minsky.translate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import minsky.cm.Assembler;
import minsky.cm.CounterMachine;
import minsky.cm.Instr;

/**
 * Máquina de k contadores -> máquina de 2 contadores (Z, Y).
 *
 * Z = p_0^c_0 * ... * p_{k-1}^c_{k-1}, Y auxiliar (0 entre macros).
 * INC c_i -> Z := Z*p_i.
 * DEC c_i -> si p_i | Z, Z := Z/p_i y rama no-cero; si no, Z se restaura y rama cero.
 * El resto de la división se conserva en el contador de programa.
 */
public final class CountersToTwo {

    public static final int Z = 0;
    public static final int Y = 1;

    private CountersToTwo() {
    }

    public static int[] primes(int n) {
        int[] out = new int[n];
        int found = 0;
        for (int c = 2; found < n; c++) {
            boolean prime = true;
            for (int i = 0; i < found && out[i] * out[i] <= c; i++) {
                if (c % out[i] == 0) {
                    prime = false;
                    break;
                }
            }
            if (prime) {
                out[found++] = c;
            }
        }
        return out;
    }

    public static CounterMachine compile(CounterMachine cm) {
        int[] p = primes(cm.registers());
        Assembler a = new Assembler("Z", "Y");
        for (int i = 0; i < cm.size(); i++) {
            Instr ins = cm.instr(i);
            a.label(List.of("I", i));
            String cp = cm.checkpointAt(i);
            if (cp != null) {
                // prefijo neutro para que el punto de control no quede dentro de un bucle
                a.checkpoint(cp);
                a.inc(Y, null);
                a.dec(Y, null, null);
            }
            switch (ins.op()) {
                case HALT -> a.halt(cm.haltInfoAt(i));
                case INC -> mul(a, p[ins.reg()], List.of("I", ins.next()));
                case DEC -> div(a, p[ins.reg()], List.of("I", ins.next()), List.of("I", ins.zero()));
            }
        }
        return a.assemble(cm.name() + " -> 2 contadores");
    }

    private static void mul(Assembler a, int p, Object next) {
        String loop = a.fresh("mul");
        String back = a.fresh("back");
        a.label(loop);
        a.dec(Z, null, back);
        for (int t = 0; t < p; t++) {
            a.inc(Y, t == p - 1 ? loop : null);
        }
        a.label(back);
        a.dec(Y, null, next);
        a.inc(Z, back);
    }

    private static void div(Assembler a, int p, Object nonZero, Object zero) {
        String loop = a.fresh("div");
        String inc = a.fresh("dinc");
        String[] rems = new String[p];
        for (int r = 0; r < p; r++) {
            rems[r] = a.fresh("rem");
        }
        a.label(loop);
        for (int t = 0; t < p; t++) {
            a.dec(Z, t == p - 1 ? inc : null, rems[t]);
        }
        a.label(inc);
        a.inc(Y, loop);

        // resto 0: Z := Y
        a.label(rems[0]);
        a.dec(Y, null, nonZero);
        a.inc(Z, rems[0]);

        // resto r > 0: Z := Y*p + r
        for (int r = 1; r < p; r++) {
            String fix = a.fresh("fix");
            a.label(rems[r]);
            a.dec(Y, null, fix);
            for (int t = 0; t < p; t++) {
                a.inc(Z, t == p - 1 ? rems[r] : null);
            }
            a.label(fix);
            a.addConst(Z, r, zero);
        }
    }

    public static BigInteger[] encode(BigInteger[] regs) {
        int[] p = primes(regs.length);
        BigInteger z = BigInteger.ONE;
        for (int i = 0; i < regs.length; i++) {
            z = z.multiply(BigInteger.valueOf(p[i]).pow(regs[i].intValueExact()));
        }
        return new BigInteger[] {z, BigInteger.ZERO};
    }

    public static BigInteger[] decode(BigInteger[] regs, int k) {
        if (regs[Y].signum() != 0) {
            throw new IllegalStateException("Y = " + regs[Y] + " en un punto de control");
        }
        BigInteger z = regs[Z];
        List<BigInteger> out = new ArrayList<>();
        for (int prime : primes(k)) {
            BigInteger bp = BigInteger.valueOf(prime);
            long e = 0;
            BigInteger[] qr = z.divideAndRemainder(bp);
            while (qr[1].signum() == 0) {
                z = qr[0];
                e++;
                qr = z.divideAndRemainder(bp);
            }
            out.add(BigInteger.valueOf(e));
        }
        if (!z.equals(BigInteger.ONE)) {
            throw new IllegalStateException("Z contiene factores ajenos a la codificación");
        }
        return out.toArray(new BigInteger[0]);
    }
}
