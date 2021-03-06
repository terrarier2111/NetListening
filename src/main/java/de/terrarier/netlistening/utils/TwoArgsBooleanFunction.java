package de.terrarier.netlistening.utils;

import org.jetbrains.annotations.ApiStatus;

/**
 * @since 1.03
 * @author Terrarier2111
 */
@ApiStatus.Internal
public interface TwoArgsBooleanFunction<F, S> {

    boolean apply(F first, S second);

}
