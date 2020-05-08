package io.kaleido;

public class Result {
    private int successes = 0;
    private int failures = 0;

    public void addSuccess() {
        successes++;
    }

    public void addFailure() {
        failures++;
    }

    public int getSuccesses() {
        return successes;
    }

    public int getFailures() {
        return failures;
    }
}