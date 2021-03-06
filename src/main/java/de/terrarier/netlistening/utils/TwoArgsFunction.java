package de.terrarier.netlistening.utils;

import org.jetbrains.annotations.ApiStatus;

/**
 * @since 1.03
 * @author Terrarier2111
 */
@ApiStatus.Internal
public interface TwoArgsFunction<F, S, R> {

    R apply(F first, S second) throws Exception;

}
