package minsky.cm;

/** Ciclo sin decrementos que puedan agotarse: la máquina no para. */
public final class NonTerminationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NonTerminationException(int pc) {
        super("ciclo infinito en pc=" + pc);
    }
}
