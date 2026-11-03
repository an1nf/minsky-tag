package minsky.cm;

/** Ciclo sin decrementos que puedan agotarse: la máquina no para. */
public final class NonTerminationException extends RuntimeException {

    public NonTerminationException(int pc) {
        super("ciclo infinito en pc=" + pc);
    }
}
