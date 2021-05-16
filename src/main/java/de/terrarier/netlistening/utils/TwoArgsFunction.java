package de.terrarier.netlistening.utils;

import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.03
 */
@ApiStatus.Internal
public interface TwoArgsFunction<F, S, R> {

    R apply(F first, S second) throws Exception;

}
