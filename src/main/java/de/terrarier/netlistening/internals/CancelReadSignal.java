package de.terrarier.netlistening.internals;

import org.jetbrains.annotations.ApiStatus;

/**
 * @since 1.06
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class CancelReadSignal extends CancelSignal {

    public final int size;

    public CancelReadSignal(int size) {
        this.size = size;
    }

}
