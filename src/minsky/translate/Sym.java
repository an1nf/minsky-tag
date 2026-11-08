package minsky.translate;

/**
 * Símbolo del 2-tag generado a partir de una MT: kind<q,o> o kind<q,o,r>.
 * x (relleno) y J (basura) no llevan estado.
 */
public record Sym(String kind, String state, char orient, int bit) {

    public static final Sym X = new Sym("x", "", ' ', -1);
    public static final Sym J = new Sym("J", "", ' ', -1);

    public static Sym of(String kind, String q, char o) {
        return new Sym(kind, q, o, -1);
    }

    public static Sym of(String kind, String q, char o, int r) {
        return new Sym(kind, q, o, r);
    }

    @Override
    public String toString() {
        if (state.isEmpty()) {
            return kind;
        }
        return kind + "<" + state + "," + orient + (bit >= 0 ? "," + bit : "") + ">";
    }
}
