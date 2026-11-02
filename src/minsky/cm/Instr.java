package minsky.cm;

/**
 * INC r, next        r := r + 1; ir a next
 * DEC r, next, zero  si r > 0: r := r - 1, ir a next; si no, ir a zero
 * HALT
 */
public record Instr(Op op, int reg, int next, int zero) {

    public enum Op { INC, DEC, HALT }

    public static Instr inc(int reg, int next) {
        return new Instr(Op.INC, reg, next, -1);
    }

    public static Instr dec(int reg, int next, int zero) {
        return new Instr(Op.DEC, reg, next, zero);
    }

    public static Instr halt() {
        return new Instr(Op.HALT, -1, -1, -1);
    }
}
