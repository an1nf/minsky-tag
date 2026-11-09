package minsky.translate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minsky.tag.CyclicTagSystem;
import minsky.tag.TagSystem;

/**
 * m-tag -> sistema de etiquetas cíclico (Cook 2004).
 * s_i se codifica en one-hot de longitud k; la lista cíclica tiene k*m producciones:
 * cod(P(s_1)), ..., cod(P(s_k)) y k*(m-1) vacías. Un paso del m-tag son k*m pasos cíclicos.
 * Los símbolos de parada reciben producción vacía.
 */
public final class TagToCyclic {

    public record Compiled<S>(CyclicTagSystem system, Map<S, String> code) {

        public int width() {
            return code.values().iterator().next().length();
        }

        public String encode(List<S> word) {
            StringBuilder sb = new StringBuilder();
            for (S s : word) {
                sb.append(code.get(s));
            }
            return sb.toString();
        }

        public List<S> decode(String bits) {
            Map<String, S> inv = new HashMap<>();
            code.forEach((s, c) -> inv.put(c, s));
            int k = width();
            if (bits.length() % k != 0) {
                throw new IllegalStateException("longitud no múltiplo de " + k);
            }
            List<S> out = new ArrayList<>();
            for (int i = 0; i < bits.length(); i += k) {
                out.add(inv.get(bits.substring(i, i + k)));
            }
            return out;
        }
    }

    private TagToCyclic() {
    }

    /** extra: símbolos que no aparecen en las reglas (p. ej. de la palabra inicial). */
    public static <S> Compiled<S> compile(TagSystem<S> ts, Collection<S> extra) {
        Set<S> set = new LinkedHashSet<>(ts.alphabet());
        set.addAll(extra);
        List<S> alphabet = new ArrayList<>(set);
        alphabet.sort(Comparator.comparing(Object::toString));
        int k = alphabet.size();
        Map<S, String> code = new LinkedHashMap<>();
        for (int i = 0; i < k; i++) {
            char[] bits = new char[k];
            Arrays.fill(bits, '0');
            bits[i] = '1';
            code.put(alphabet.get(i), new String(bits));
        }
        List<String> apps = new ArrayList<>();
        for (S s : alphabet) {
            StringBuilder sb = new StringBuilder();
            for (S t : ts.rules().getOrDefault(s, List.of())) {
                sb.append(code.get(t));
            }
            apps.add(sb.toString());
        }
        for (int i = 0; i < k * (ts.deletion() - 1); i++) {
            apps.add("");
        }
        return new Compiled<>(new CyclicTagSystem(apps), code);
    }
}
