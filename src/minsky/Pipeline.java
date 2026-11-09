package minsky;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import minsky.cm.CounterMachine;
import minsky.cm.Result;
import minsky.tag.TagResult;
import minsky.tag.TagSystem;
import minsky.tm.Config;
import minsky.tm.Run;
import minsky.tm.TuringMachine;
import minsky.translate.CountersToTwo;
import minsky.translate.Sym;
import minsky.translate.TagToCyclic;
import minsky.translate.TmToCounters;
import minsky.translate.TmToTag;

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

    public static Report runTwoCounters(TuringMachine tm, int[] input, int maxTmSteps, boolean accelerate) {
        CounterMachine cm2 = CountersToTwo.compile(TmToCounters.compile(tm));
        List<Config> configs = new ArrayList<>();
        BigInteger[] zMax = {BigInteger.ZERO};
        BigInteger[] start = CountersToTwo.encode(TmToCounters.registers(tm.initial(input)));
        Result res = cm2.run(start, null, accelerate, (q, regs, s) -> {
            zMax[0] = zMax[0].max(regs[CountersToTwo.Z]);
            configs.add(TmToCounters.decode(q, CountersToTwo.decode(regs, 3)));
            return configs.size() <= maxTmSteps;
        });
        Config last = null;
        if (res.halted()) {
            zMax[0] = zMax[0].max(res.regs()[CountersToTwo.Z]);
            last = TmToCounters.decode(res.haltInfo(), CountersToTwo.decode(res.regs(), 3));
        }
        return new Report("2 contadores", configs, last, res.halted(), res.steps(), cm2.size(), zMax[0], 0);
    }

    public static Report runTag(TuringMachine tm, int[] input, int maxTmSteps) {
        TagSystem<Sym> ts = TmToTag.compile(tm);
        List<Config> configs = new ArrayList<>();
        TagResult<Sym> res = ts.run(TmToTag.encode(tm.initial(input)), Long.MAX_VALUE, (w, s) -> {
            if (TmToTag.isCanonical(w)) {
                configs.add(TmToTag.decodeCanonical(w));
                return configs.size() <= maxTmSteps;
            }
            return true;
        });
        Config last = res.halted() ? TmToTag.decodeHalted(res.word()) : null;
        return new Report("2-tag", configs, last, res.halted(), BigInteger.valueOf(res.steps()),
            ts.size(), null, res.maxLength());
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

    /** Comprueba tagSteps pasos del tag contra el sistema cíclico; devuelve los pasos cíclicos. */
    public static <S> long cyclicCheck(TagSystem<S> ts, List<S> word, long tagSteps) {
        TagToCyclic.Compiled<S> c = TagToCyclic.compile(ts, word);
        int period = c.width() * ts.deletion();
        List<S> w = new ArrayList<>(word);
        String bits = c.encode(w);
        long cyclic = 0;
        for (long t = 0; t < tagSteps; t++) {
            List<S> p = w.size() < ts.deletion() ? null : ts.rules().get(w.get(0));
            if (p == null) {
                break;
            }
            List<S> next = new ArrayList<>(w.subList(ts.deletion(), w.size()));
            next.addAll(p);
            w = next;
            // el puntero cíclico vuelve a 0 en cada frontera: k*m = longitud de la lista
            bits = c.system().run(bits, period);
            cyclic += period;
            if (!c.decode(bits).equals(w)) {
                throw new AssertionError("el sistema cíclico se desvía en el paso " + t);
            }
        }
        return cyclic;
    }
}
