package minsky;

import java.util.List;

import minsky.tm.TuringMachine;

/** MT de ejemplo. La cabeza empieza sobre input[0]. */
public final class Examples {

    public record Example(TuringMachine tm, int[] input) {
    }

    private Examples() {
    }

    /** Castor afanoso de 2 estados: 4 unos en 6 pasos. */
    public static TuringMachine busyBeaver2() {
        return new TuringMachine("castor-2", "A")
            .on("A", 0, "B", 1, 'R').on("A", 1, "B", 1, 'L')
            .on("B", 0, "A", 1, 'L').on("B", 1, "H", 1, 'R');
    }

    /** Castor afanoso de 3 estados: 6 unos en 14 pasos. */
    public static TuringMachine busyBeaver3() {
        return new TuringMachine("castor-3", "A")
            .on("A", 0, "B", 1, 'R').on("A", 1, "H", 1, 'R')
            .on("B", 0, "C", 0, 'R').on("B", 1, "B", 1, 'R')
            .on("C", 0, "C", 1, 'L').on("C", 1, "A", 1, 'L');
    }

    /**
     * Incremento binario, bit menos significativo bajo la cabeza.
     * Escribe un 1 de tope en la celda -1 para poder volver y lo borra al final.
     */
    public static TuringMachine binaryIncrement() {
        return new TuringMachine("incremento-binario", "ini")
            .on("ini", 0, "pon", 0, 'L').on("ini", 1, "pon", 1, 'L')
            .on("pon", 0, "inc", 1, 'R')
            .on("inc", 1, "inc", 0, 'R').on("inc", 0, "vuelve", 1, 'L')
            .on("vuelve", 0, "vuelve", 0, 'L').on("vuelve", 1, "tope", 1, 'L')
            .on("tope", 0, "quita", 0, 'R').on("tope", 1, "tope", 1, 'L')
            .on("quita", 1, "fin", 0, 'R');
    }

    /** 1^a 0 1^b -> 1^(a+b). */
    public static TuringMachine unaryAddition() {
        return new TuringMachine("suma-unaria", "s0")
            .on("s0", 1, "s0", 1, 'R').on("s0", 0, "s1", 1, 'R')
            .on("s1", 1, "s1", 1, 'R').on("s1", 0, "s2", 0, 'L')
            .on("s2", 1, "H", 0, 'L');
    }

    /** Incremento en base 3, dígito menos significativo bajo la cabeza. */
    public static TuringMachine ternaryIncrement() {
        return new TuringMachine("incremento-ternario", "inc", 3)
            .on("inc", 0, "H", 1, 'L').on("inc", 1, "H", 2, 'L')
            .on("inc", 2, "inc", 0, 'R');
    }

    public static List<Example> all() {
        return List.of(
            new Example(busyBeaver2(), new int[] {}),
            new Example(busyBeaver3(), new int[] {}),
            new Example(binaryIncrement(), new int[] {1, 1, 0, 1}),
            new Example(unaryAddition(), new int[] {1, 1, 1, 0, 1, 1}),
            new Example(ternaryIncrement(), new int[] {2, 2, 1}));
    }
}
