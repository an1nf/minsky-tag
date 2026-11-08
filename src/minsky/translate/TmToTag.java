package minsky.translate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minsky.tag.TagSystem;
import minsky.tm.Config;
import minsky.tm.Transition;
import minsky.tm.TuringMachine;

/**
 * MT binaria -> sistema 2-tag (esquema de Cocke y Minsky 1964, con orientación).
 *
 * Configuración (q, o, u, v): v contiene la cabeza en su bit bajo, u es el otro lado.
 *   o = R: L = u, R = v.      o = L: L = v div 2, R = 2u + v mod 2.
 * Palabra canónica: A x (a x)^u B x (b x)^v, símbolos indexados por S = (q, o).
 *
 * Pasada 1: A -> C x, a -> c x, B -> D, b -> d.
 * Pasada 2: K -> K1 K0 para K en {C, c, D, d}. El bloque D d^v mide v + 1; si v es par
 *           la lectura se desfasa y en la pasada 3 se leen los símbolos con índice 0.
 * Pasada 3: con r = v mod 2 conocido, se construye la configuración siguiente.
 *           Si r = 0, C_r empieza por J, que absorbe el desfase.
 * Pasada 4: sólo tras un giro, rota la palabra (Bt -> B x, bt -> b x).
 */
public final class TmToTag {

    private static final String[] SPLIT = {"C", "c", "D", "d"};

    private TmToTag() {
    }

    private static char flip(char o) {
        return o == 'R' ? 'L' : 'R';
    }

    public static TagSystem<Sym> compile(TuringMachine tm) {
        if (tm.base() != 2) {
            throw new IllegalArgumentException("se requiere una MT binaria");
        }
        Map<Sym, List<Sym>> p = new LinkedHashMap<>();
        for (String q : tm.states()) {
            for (char o : new char[] {'R', 'L'}) {
                p.put(Sym.of("A", q, o), List.of(Sym.of("C", q, o), Sym.X));
                p.put(Sym.of("a", q, o), List.of(Sym.of("c", q, o), Sym.X));
                p.put(Sym.of("B", q, o), List.of(Sym.of("D", q, o)));
                p.put(Sym.of("b", q, o), List.of(Sym.of("d", q, o)));
                for (String k : SPLIT) {
                    p.put(Sym.of(k, q, o), List.of(Sym.of(k, q, o, 1), Sym.of(k, q, o, 0)));
                }
                p.put(Sym.of("Bt", q, o), List.of(Sym.of("B", q, o), Sym.X));
                p.put(Sym.of("bt", q, o), List.of(Sym.of("b", q, o), Sym.X));
                for (int r = 0; r <= 1; r++) {
                    Transition tr = tm.transition(q, r);
                    if (tr != null) {
                        third(p, q, o, r, tr);
                    }
                }
            }
        }
        return new TagSystem<>(tm.name() + " -> 2-tag", 2, p);
    }

    private static void third(Map<Sym, List<Sym>> p, String q, char o, int r, Transition tr) {
        String q2 = tr.next();
        int w = tr.write();
        List<Sym> c = new ArrayList<>();
        if (r == 0) {
            c.add(Sym.J);
        }
        if (tr.move() == o) {
            // seguir: (q', o, 2u + w, v div 2)
            c.addAll(List.of(Sym.of("A", q2, o), Sym.X));
            repeat(c, Sym.of("a", q2, o), w);
            p.put(Sym.of("C", q, o, r), c);
            p.put(Sym.of("c", q, o, r), pairs(Sym.of("a", q2, o), 2));
            p.put(Sym.of("D", q, o, r), List.of(Sym.of("B", q2, o), Sym.X));
            p.put(Sym.of("d", q, o, r), List.of(Sym.of("b", q2, o), Sym.X));
        } else {
            // girar: (q', ō, 2(v div 2) + w, u), con rotación posterior
            char o2 = flip(o);
            c.addAll(List.of(Sym.of("Bt", q2, o2), Sym.X));
            p.put(Sym.of("C", q, o, r), c);
            p.put(Sym.of("c", q, o, r), List.of(Sym.of("bt", q2, o2), Sym.X));
            List<Sym> d = new ArrayList<>(List.of(Sym.of("A", q2, o2), Sym.X));
            repeat(d, Sym.of("a", q2, o2), w);
            p.put(Sym.of("D", q, o, r), d);
            p.put(Sym.of("d", q, o, r), pairs(Sym.of("a", q2, o2), 2));
        }
    }

    private static void repeat(List<Sym> out, Sym s, int n) {
        for (int i = 0; i < n; i++) {
            out.add(s);
            out.add(Sym.X);
        }
    }

    private static List<Sym> pairs(Sym s, int n) {
        List<Sym> out = new ArrayList<>();
        repeat(out, s, n);
        return out;
    }

    /** Palabra canónica en orientación R: u = L, v = R. */
    public static List<Sym> encode(Config c) {
        String q = c.state();
        List<Sym> w = new ArrayList<>(List.of(Sym.of("A", q, 'R'), Sym.X));
        repeat(w, Sym.of("a", q, 'R'), c.left().intValueExact());
        w.add(Sym.of("B", q, 'R'));
        w.add(Sym.X);
        repeat(w, Sym.of("b", q, 'R'), c.right().intValueExact());
        return w;
    }

    public static boolean isCanonical(Collection<Sym> word) {
        return !word.isEmpty() && word.iterator().next().kind().equals("A");
    }

    private static Config toConfig(String q, char o, long u, long v) {
        BigInteger bu = BigInteger.valueOf(u);
        BigInteger bv = BigInteger.valueOf(v);
        if (o == 'R') {
            return new Config(q, bu, bv);
        }
        return new Config(q, bv.shiftRight(1), bu.shiftLeft(1).add(BigInteger.valueOf(v & 1)));
    }

    /** Decodifica y valida A x (a x)^u B x (b x)^v. */
    public static Config decodeCanonical(Collection<Sym> word) {
        List<Sym> w = new ArrayList<>(word);
        Sym head = w.get(0);
        String q = head.state();
        char o = head.orient();
        Sym a = Sym.of("a", q, o);
        Sym b = Sym.of("b", q, o);
        if (!w.get(1).equals(Sym.X)) {
            throw new IllegalStateException("palabra no canónica");
        }
        int i = 2;
        long u = 0;
        long v = 0;
        while (w.get(i).equals(a)) {
            check(w.get(i + 1).equals(Sym.X));
            u++;
            i += 2;
        }
        check(w.get(i).equals(Sym.of("B", q, o)) && w.get(i + 1).equals(Sym.X));
        i += 2;
        while (i < w.size()) {
            check(w.get(i).equals(b) && w.get(i + 1).equals(Sym.X));
            v++;
            i += 2;
        }
        return toConfig(q, o, u, v);
    }

    /** La MT para cuando el sistema lee C<q,o,r> sin producción. */
    public static Config decodeHalted(Collection<Sym> word) {
        Sym head = word.iterator().next();
        check(head.kind().equals("C") && head.bit() >= 0);
        Sym c = Sym.of("c", head.state(), head.orient(), head.bit());
        Sym d = Sym.of("d", head.state(), head.orient(), head.bit());
        long u = word.stream().filter(c::equals).count();
        long vh = word.stream().filter(d::equals).count();
        return toConfig(head.state(), head.orient(), u, 2 * vh + head.bit());
    }

    private static void check(boolean ok) {
        if (!ok) {
            throw new IllegalStateException("palabra no canónica");
        }
    }
}
