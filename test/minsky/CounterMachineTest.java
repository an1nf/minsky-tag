package minsky;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Test;

import minsky.cm.Assembler;
import minsky.cm.CounterMachine;
import minsky.cm.Instr;
import minsky.cm.NonTerminationException;
import minsky.cm.Result;

public class CounterMachineTest {

    private static BigInteger[] big(long... v) {
        BigInteger[] r = new BigInteger[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = BigInteger.valueOf(v[i]);
        }
        return r;
    }

    private static void assertSameRun(String msg, Result a, Result b) {
        assertArrayEquals(msg, a.regs(), b.regs());
        assertEquals(msg, a.steps(), b.steps());
        assertEquals(msg, a.pc(), b.pc());
        assertEquals(msg, a.halted(), b.halted());
    }

    @Test
    public void moveAddsMultiple() {
        Assembler a = new Assembler("X", "Y");
        a.move(0, 1, 3, "fin");
        a.label("fin");
        a.halt(null);
        Result r = a.assemble("mv").run(5, 2);
        assertTrue(r.halted());
        assertArrayEquals(big(0, 17), r.regs());
        assertEquals(BigInteger.valueOf(4 * 5 + 1), r.steps());
    }

    @Test
    public void accelerationIsExact() {
        BigInteger limit = BigInteger.valueOf(5_000);
        for (int seed = 0; seed < 300; seed++) {
            Random rng = new Random(seed);
            CounterMachine cm = RandomMachines.program(rng, 1 + rng.nextInt(3), 2 + rng.nextInt(11));
            BigInteger[] regs = new BigInteger[cm.registers()];
            for (int i = 0; i < regs.length; i++) {
                regs[i] = BigInteger.valueOf(rng.nextInt(31));
            }
            Result naive = cm.run(regs, limit, false, null);
            Result fast = cm.run(regs, limit, true, null);
            assertSameRun("semilla " + seed, naive, fast);
        }
    }

    @Test(expected = NonTerminationException.class)
    public void detectsNonTermination() {
        CounterMachine.of("bucle", 1, Instr.inc(0, 0)).run(0);
    }
}
