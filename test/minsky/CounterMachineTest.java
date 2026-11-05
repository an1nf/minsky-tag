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
import minsky.translate.CountersToTwo;

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
    public void primes() {
        assertArrayEquals(new int[] {2, 3, 5, 7, 11, 13}, CountersToTwo.primes(6));
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

    @Test
    public void godelRoundTrip() {
        BigInteger[] regs = big(3, 0, 7);
        assertArrayEquals(regs, CountersToTwo.decode(CountersToTwo.encode(regs), 3));
    }

    @Test
    public void twoCountersMatchRandomPrograms() {
        int checked = 0;
        for (int seed = 0; seed < 400; seed++) {
            Random rng = new Random(1_000 + seed);
            int k = 1 + rng.nextInt(3);
            CounterMachine cm = RandomMachines.program(rng, k, 2 + rng.nextInt(9));
            BigInteger[] regs = new BigInteger[k];
            for (int i = 0; i < k; i++) {
                regs[i] = BigInteger.valueOf(rng.nextInt(4));
            }
            Result ref = cm.run(regs, BigInteger.valueOf(2_000), false, null);
            if (!ref.halted()) {
                continue;
            }
            CounterMachine cm2 = CountersToTwo.compile(cm);
            Result res = cm2.run(CountersToTwo.encode(regs), BigInteger.TEN.pow(30), true, null);
            assertTrue("semilla " + seed, res.halted());
            assertArrayEquals("semilla " + seed, ref.regs(), CountersToTwo.decode(res.regs(), k));
            checked++;
        }
        assertTrue(checked > 200);
    }

    @Test
    public void twoCountersAccelerationIsExact() {
        BigInteger limit = BigInteger.valueOf(200_000);
        for (int seed = 0; seed < 40; seed++) {
            Random rng = new Random(5_000 + seed);
            CounterMachine cm2 = CountersToTwo.compile(RandomMachines.program(rng, 2, 2 + rng.nextInt(5)));
            BigInteger[] regs = CountersToTwo.encode(big(rng.nextInt(3), rng.nextInt(3)));
            assertSameRun("semilla " + seed, cm2.run(regs, limit, false, null), cm2.run(regs, limit, true, null));
        }
    }
}
