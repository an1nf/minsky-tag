package minsky.tag;

import java.util.List;

public record TagResult<S>(List<S> word, long steps, boolean halted, int maxLength,
                           boolean stoppedByObserver) {
}
