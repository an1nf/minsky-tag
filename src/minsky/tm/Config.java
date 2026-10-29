package minsky.tm;

import java.math.BigInteger;
import java.util.List;

/**
 * Configuración (q, L, R).
 * L: celdas a la izquierda de la cabeza, la más cercana es el dígito menos significativo.
 * R: celda leída y celdas a su derecha; el símbolo leído es R mod b.
 */
public record Config(String state, BigInteger left, BigInteger right) {

    public static Config of(String state, long left, long right) {
        return new Config(state, BigInteger.valueOf(left), BigInteger.valueOf(right));
    }

    /** Cinta en orden físico con la celda leída entre corchetes. */
    public String tape(int base) {
        List<Integer> l = TuringMachine.digits(left, base);
        List<Integer> r = TuringMachine.digits(right, base);
        StringBuilder sb = new StringBuilder();
        for (int i = l.size() - 1; i >= 0; i--) {
            sb.append(l.get(i));
        }
        sb.append('[').append(r.isEmpty() ? 0 : r.get(0)).append(']');
        for (int i = 1; i < r.size(); i++) {
            sb.append(r.get(i));
        }
        return sb.toString();
    }
}
