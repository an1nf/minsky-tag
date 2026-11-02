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
import minsky.cm.Result;

public class CounterMachineTest {

    private static BigInteger[] big(long... v) {
        BigInteger[] r = new BigInteger[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = BigInteger.valueOf(v[i]);
        }
        return r;
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
}
