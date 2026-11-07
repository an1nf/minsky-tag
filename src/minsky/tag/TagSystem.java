package minsky.tag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * m-tag de Post. Paso: leer el primer símbolo a, añadir P(a) al final, borrar m símbolos.
 * Para si la palabra mide menos de m o si a no tiene producción.
 */
public final class TagSystem<S> {

    @FunctionalInterface
    public interface Observer<S> {
        /** Se invoca antes de cada paso y con la palabra final; false detiene. */
        boolean visit(Deque<S> word, long steps);
    }

    private final String name;
    private final int m;
    private final Map<S, List<S>> rules;

    public TagSystem(String name, int m, Map<S, List<S>> rules) {
        if (m < 1) {
            throw new IllegalArgumentException("m < 1");
        }
        this.name = name;
        this.m = m;
        this.rules = Collections.unmodifiableMap(new LinkedHashMap<>(rules));
    }

    /** Sistema sobre caracteres: rules = {'a' -> "bc", ...}. */
    public static TagSystem<Character> ofStrings(String name, int m, Map<Character, String> rules) {
        Map<Character, List<Character>> r = new LinkedHashMap<>();
        rules.forEach((k, v) -> r.put(k, chars(v)));
        return new TagSystem<>(name, m, r);
    }

    public static List<Character> chars(String s) {
        List<Character> out = new ArrayList<>(s.length());
        for (char c : s.toCharArray()) {
            out.add(c);
        }
        return out;
    }

    public String name() {
        return name;
    }

    public int deletion() {
        return m;
    }

    public Map<S, List<S>> rules() {
        return rules;
    }

    public int size() {
        return rules.size();
    }

    public Set<S> alphabet() {
        Set<S> a = new LinkedHashSet<>(rules.keySet());
        rules.values().forEach(a::addAll);
        return a;
    }

    public TagResult<S> run(List<S> word, long maxSteps, Observer<S> obs) {
        Deque<S> w = new ArrayDeque<>(word);
        long steps = 0;
        int maxLen = w.size();
        while (true) {
            if (obs != null && !obs.visit(w, steps)) {
                return new TagResult<>(new ArrayList<>(w), steps, false, maxLen, true);
            }
            List<S> p = w.size() < m ? null : rules.get(w.peekFirst());
            if (p == null) {
                return new TagResult<>(new ArrayList<>(w), steps, true, maxLen, false);
            }
            if (steps >= maxSteps) {
                return new TagResult<>(new ArrayList<>(w), steps, false, maxLen, false);
            }
            w.addAll(p);
            for (int i = 0; i < m; i++) {
                w.pollFirst();
            }
            steps++;
            maxLen = Math.max(maxLen, w.size());
        }
    }

    public TagResult<S> run(List<S> word) {
        return run(word, Long.MAX_VALUE, null);
    }

    public String listing() {
        StringBuilder sb = new StringBuilder();
        sb.append("; ").append(name).append(": ").append(m).append("-tag, ")
          .append(rules.size()).append(" producciones, ")
          .append(alphabet().size()).append(" símbolos\n");
        rules.forEach((k, v) -> {
            StringBuilder rhs = new StringBuilder();
            for (S s : v) {
                rhs.append(rhs.length() == 0 ? "" : " ").append(s);
            }
            sb.append(String.format("%22s -> %s%n", k, rhs.length() == 0 ? "ε" : rhs));
        });
        return sb.toString();
    }
}
