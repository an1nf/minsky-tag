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
import minsky.translate.Sym;
import minsky.translate.TmToTag;

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

    @Test
    public void cyclicCollatz() {
        for (int n : new int[] {3, 7, 12, 27}) {
            assertTrue(Pipeline.cyclicCheck(COLLATZ, TagSystem.chars("a".repeat(n)), 200) > 0);
        }
    }

    @Test
    public void cyclicOnCompiledTuringMachine() {
        TuringMachine tm = Examples.busyBeaver2();
        TagSystem<Sym> ts = TmToTag.compile(tm);
        assertTrue(Pipeline.cyclicCheck(ts, TmToTag.encode(tm.initial(new int[] {})), 200) > 0);
    }

    @Test
    public void cyclicRandom() {
        String letters = "abcd";
        for (int seed = 0; seed < 30; seed++) {
            Random rng = new Random(seed);
            int m = 2 + rng.nextInt(2);
            String alpha = letters.substring(0, 2 + rng.nextInt(3));
            Map<Character, String> rules = new LinkedHashMap<>();
            for (char s : alpha.toCharArray()) {
                if (rng.nextDouble() < 0.9) {
                    StringBuilder p = new StringBuilder();
                    for (int i = rng.nextInt(5); i > 0; i--) {
                        p.append(alpha.charAt(rng.nextInt(alpha.length())));
                    }
                    rules.put(s, p.toString());
                }
            }
            StringBuilder w = new StringBuilder();
            for (int i = m + rng.nextInt(9 - m); i > 0; i--) {
                w.append(alpha.charAt(rng.nextInt(alpha.length())));
            }
            Pipeline.cyclicCheck(TagSystem.ofStrings("aleatorio", m, rules), TagSystem.chars(w.toString()), 60);
        }
    }

    @Test
    public void productionCount() {
        for (Examples.Example ex : Examples.all()) {
            TuringMachine tm = ex.tm();
            if (tm.base() == 2) {
                int expected = 20 * tm.states().size() + 8 * tm.transitionCount();
                assertEquals(tm.name(), expected, TmToTag.compile(tm).size());
            }
        }
    }
}
