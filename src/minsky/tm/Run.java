package minsky.tm;

import java.util.List;
import java.util.Map;

public record Run(List<Config> trace, boolean halted, Map<Integer, Integer> tape, int head) {

    public int steps() {
        return trace.size() - 1;
    }

    public Config last() {
        return trace.get(trace.size() - 1);
    }
}
