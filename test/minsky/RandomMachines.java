package minsky;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import minsky.tm.TuringMachine;

/** Generadores de máquinas aleatorias para las pruebas diferenciales. */
final class RandomMachines {

    private RandomMachines() {
    }

    static TuringMachine turing(Random rng, int base) {
        int n = 1 + rng.nextInt(4);
        List<String> states = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            states.add("q" + i);
        }
        List<String> targets = new ArrayList<>(states);
        targets.add("H");
        TuringMachine tm = new TuringMachine("aleatoria", "q0", base);
        for (String q : states) {
            for (int s = 0; s < base; s++) {
                if (rng.nextDouble() < 0.85) {
                    tm.on(q, s, targets.get(rng.nextInt(targets.size())), rng.nextInt(base),
                        rng.nextBoolean() ? 'L' : 'R');
                }
            }
        }
        return tm;
    }

    static int[] tape(Random rng, int base, int maxLen) {
        int[] t = new int[rng.nextInt(maxLen + 1)];
        for (int i = 0; i < t.length; i++) {
            t[i] = rng.nextInt(base);
        }
        return t;
    }
}
