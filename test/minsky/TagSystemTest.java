package minsky;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import minsky.tag.TagResult;
import minsky.tag.TagSystem;
import minsky.tm.TuringMachine;

public class TagSystemTest {

    /** 2-tag de De Mol (2008): a^n -> a^(n/2) si n es par, a^((3n+1)/2) si es impar. */
    private static final TagSystem<Character> COLLATZ =
        TagSystem.ofStrings("collatz", 2, Map.of('a', "bc", 'b', "a", 'c', "aaa"));

    @Test
    public void collatzDeMol() {
        for (int n = 2; n < 60; n++) {
            List<Integer> seen = new ArrayList<>();
            COLLATZ.run(TagSystem.chars("a".repeat(n)), Long.MAX_VALUE, (w, s) -> {
                if (w.stream().allMatch(c -> c == 'a')) {
                    seen.add(w.size());
                }
                return true;
            });
            List<Integer> expected = new ArrayList<>(List.of(n));
            for (int x = n; x != 1; ) {
                x = x % 2 == 0 ? x / 2 : (3 * x + 1) / 2;
                expected.add(x);
            }
            assertEquals("n = " + n, expected, seen);
        }
    }

    @Test
    public void haltsOnShortWord() {
        TagResult<Character> r = COLLATZ.run(TagSystem.chars("a"));
        assertTrue(r.halted());
        assertEquals(0, r.steps());
    }
}
