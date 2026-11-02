package minsky.cm;

import java.math.BigInteger;

public record Result(BigInteger[] regs, BigInteger steps, boolean halted, int pc,
                     String haltInfo, boolean stoppedByObserver) {
}
