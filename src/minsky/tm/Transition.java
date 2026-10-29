package minsky.tm;

/** delta(q, s) = (next, write, move), con move en {'L', 'R'}. */
public record Transition(String next, int write, char move) {
}
