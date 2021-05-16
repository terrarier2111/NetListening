package de.terrarier.netlistening.utils;

import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.03
 */
@ApiStatus.Internal
public interface TwoArgsBooleanFunction<F, S> {

    boolean apply(F first, S second);

}
