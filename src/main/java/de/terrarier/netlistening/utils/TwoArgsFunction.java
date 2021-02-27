package de.terrarier.netlistening.utils;

public interface TwoArgsFunction<F, S, R> {

    R apply(F first, S second) throws Exception;

}
