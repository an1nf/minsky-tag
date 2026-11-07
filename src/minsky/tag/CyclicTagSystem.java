package minsky.tag;

import java.util.List;

/**
 * Sistema de etiquetas cíclico (Cook 2004). En el paso t se borra el primer bit
 * y, si era 1, se añade appendants[t mod N].
 */
public final class CyclicTagSystem {

    private final List<String> appendants;

    public CyclicTagSystem(List<String> appendants) {
        this.appendants = List.copyOf(appendants);
    }

    public List<String> appendants() {
        return appendants;
    }

    /** Ejecuta `steps` pasos empezando con el puntero cíclico en 0. */
    public String run(String word, long steps) {
        StringBuilder w = new StringBuilder(word);
        int head = 0;
        int n = appendants.size();
        for (long t = 0; t < steps && head < w.length(); t++) {
            if (w.charAt(head++) == '1') {
                w.append(appendants.get((int) (t % n)));
            }
            if (head > 4096 && head > w.length() / 2) {
                w.delete(0, head);
                head = 0;
            }
        }
        return w.substring(head);
    }
}
