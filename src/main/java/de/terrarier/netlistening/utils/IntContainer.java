package de.terrarier.netlistening.utils;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class IntContainer {

    private int value;

    public int get() {
        return value;
    }

    public void add(int increment) {
        value += increment;
    }

    public int getAndAdd(int increment) {
        final int formerValue = value;
        value += increment;
        return formerValue;
    }

}
