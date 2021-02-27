package de.terrarier.netlistening.utils;

public interface TwoArgsBooleanFunction<F, S> {

    boolean apply(F first, S second);

}
