package minsky;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import minsky.cm.CounterMachine;
import minsky.cm.Result;
import minsky.tm.Config;
import minsky.tm.Run;
import minsky.tm.TuringMachine;
import minsky.translate.TmToCounters;

/**
 * Ejecuta cada traducción y decodifica la configuración de la MT en sus puntos
 * de control, para compararla con la traza de la MT de referencia.
 */
public final class Pipeline {

    /** last es null si la simulación no paró. */
    public record Report(String model, List<Config> configs, Config last, boolean halted,
                         BigInteger steps, int size, BigInteger zMax, int maxLength) {
    }

    private Pipeline() {
    }

    public static Report runCounters(TuringMachine tm, int[] input, int maxTmSteps, boolean accelerate) {
        CounterMachine cm = TmToCounters.compile(tm);
        List<Config> configs = new ArrayList<>();
        Result res = cm.run(TmToCounters.registers(tm.initial(input)), null, accelerate,
            (q, regs, s) -> {
                configs.add(TmToCounters.decode(q, regs));
                return configs.size() <= maxTmSteps;
            });
        Config last = res.halted() ? TmToCounters.decode(res.haltInfo(), res.regs()) : null;
        return new Report("3 contadores", configs, last, res.halted(), res.steps(), cm.size(), null, 0);
    }

    /** Lanza AssertionError si la simulación no reproduce la traza de la MT. */
    public static void check(TuringMachine tm, int[] input, Report rep, int maxTmSteps) {
        Run ref = tm.run(input, maxTmSteps);
        int n = Math.min(ref.trace().size(), maxTmSteps + 1);
        for (int i = 0; i < n; i++) {
            Config got = i < rep.configs().size() ? rep.configs().get(i) : null;
            if (!ref.trace().get(i).equals(got)) {
                throw new AssertionError(rep.model() + ": paso " + i + ": " + got
                    + " != " + ref.trace().get(i));
            }
        }
        if (ref.halted() && ref.steps() <= maxTmSteps) {
            if (!rep.halted()) {
                throw new AssertionError(rep.model() + ": la MT para y la simulación no");
            }
            if (!ref.last().equals(rep.last())) {
                throw new AssertionError(rep.model() + ": configuración final " + rep.last()
                    + " != " + ref.last());
            }
            if (rep.configs().size() != ref.steps() + 1) {
                throw new AssertionError(rep.model() + ": puntos de control de más");
            }
        }
    }
}
