package minsky;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import minsky.cm.CounterMachine;
import minsky.tm.Config;
import minsky.tm.Run;
import minsky.tm.TuringMachine;
import minsky.translate.TmToCounters;

/** MT de referencia frente a 3 contadores, 2 contadores y 2-tag, paso a paso. */
public class TranslationTest {

    private static int ones(Run r) {
        return r.tape().values().stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    public void examplesComputeExpectedResults() {
        Run r = Examples.busyBeaver2().run(new int[] {}, 100);
        assertEquals(6, r.steps());
        assertEquals(4, ones(r));
        r = Examples.busyBeaver3().run(new int[] {}, 100);
        assertTrue(r.halted());
        assertEquals(14, r.steps());
        assertEquals(6, ones(r));
        r = Examples.binaryIncrement().run(new int[] {1, 1, 0, 1}, 100);
        assertEquals(Config.of("fin", 0, 12), r.last());
        r = Examples.unaryAddition().run(new int[] {1, 1, 1, 0, 1, 1}, 100);
        assertEquals(5, ones(r));
        r = Examples.ternaryIncrement().run(new int[] {2, 2, 1}, 100);
        int value = r.tape().entrySet().stream()
            .mapToInt(e -> e.getValue() * (int) Math.pow(3, e.getKey())).sum();
        assertEquals(18, value);
    }

    @Test
    public void arithmeticStepMatchesTape() {
        for (int seed = 0; seed < 100; seed++) {
            Random rng = new Random(seed);
            int base = 2 + rng.nextInt(2);
            TuringMachine tm = RandomMachines.turing(rng, base);
            int[] in = RandomMachines.tape(rng, base, 5);
            Run ref = tm.run(in, 40);
            Config c = tm.initial(in);
            assertEquals(ref.trace().get(0), c);
            for (int i = 1; i < ref.trace().size(); i++) {
                c = tm.step(c);
                assertEquals("semilla " + seed + ", paso " + i, ref.trace().get(i), c);
            }
            if (ref.halted()) {
                assertNull(tm.step(c));
            }
        }
    }

    @Test
    public void examplesThreeCounters() {
        for (Examples.Example ex : Examples.all()) {
            Pipeline.check(ex.tm(), ex.input(), Pipeline.runCounters(ex.tm(), ex.input(), 10_000, true), 10_000);
        }
    }

    @Test
    public void examplesThreeCountersWithoutAcceleration() {
        for (Examples.Example ex : Examples.all()) {
            Pipeline.Report slow = Pipeline.runCounters(ex.tm(), ex.input(), 10_000, false);
            Pipeline.Report fast = Pipeline.runCounters(ex.tm(), ex.input(), 10_000, true);
            Pipeline.check(ex.tm(), ex.input(), slow, 10_000);
            assertEquals(slow.steps(), fast.steps());
            assertEquals(slow.last(), fast.last());
        }
    }

    @Test
    public void examplesTwoCounters() {
        for (Examples.Example ex : Examples.all()) {
            Pipeline.check(ex.tm(), ex.input(), Pipeline.runTwoCounters(ex.tm(), ex.input(), 10_000, true), 10_000);
        }
    }

    @Test
    public void examplesTwoTag() {
        for (Examples.Example ex : Examples.all()) {
            if (ex.tm().base() == 2) {
                Pipeline.check(ex.tm(), ex.input(), Pipeline.runTag(ex.tm(), ex.input(), 10_000), 10_000);
            }
        }
    }

    @Test
    public void randomThreeCounters() {
        for (int seed = 0; seed < 150; seed++) {
            Random rng = new Random(10_000 + seed);
            TuringMachine tm = RandomMachines.turing(rng, 2 + rng.nextInt(3));
            int[] in = RandomMachines.tape(rng, tm.base(), 4);
            Pipeline.check(tm, in, Pipeline.runCounters(tm, in, 25, true), 25);
        }
    }

    @Test
    public void randomTwoCounters() {
        for (int seed = 0; seed < 80; seed++) {
            Random rng = new Random(20_000 + seed);
            TuringMachine tm = RandomMachines.turing(rng, 2 + rng.nextInt(2));
            int[] in = RandomMachines.tape(rng, tm.base(), 3);
            Pipeline.check(tm, in, Pipeline.runTwoCounters(tm, in, 7, true), 7);
        }
    }

    @Test
    public void randomTwoTag() {
        for (int seed = 0; seed < 150; seed++) {
            Random rng = new Random(30_000 + seed);
            TuringMachine tm = RandomMachines.turing(rng, 2);
            int[] in = RandomMachines.tape(rng, 2, 4);
            Pipeline.check(tm, in, Pipeline.runTag(tm, in, 12), 12);
        }
    }

    @Test
    public void auxiliaryIsZeroAtCheckpoints() {
        TuringMachine tm = Examples.busyBeaver3();
        CounterMachine cm = TmToCounters.compile(tm);
        List<BigInteger> seen = new ArrayList<>();
        cm.run(TmToCounters.registers(tm.initial(new int[] {})), null, true, (q, regs, s) -> {
            seen.add(regs[TmToCounters.T]);
            return true;
        });
        assertEquals(15, seen.size());
        assertTrue(seen.stream().allMatch(t -> t.signum() == 0));
    }
}
