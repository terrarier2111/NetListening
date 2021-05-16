package de.terrarier.netlistening.internals;

import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.06
 */
@ApiStatus.Internal
public class CancelSignal extends Exception {

    public static final CancelSignal INSTANCE = new CancelSignal();

    CancelSignal() {}

    @AssumeNotNull
    @Override
    public Throwable initCause(Throwable cause) {
        return this;
    }

    @AssumeNotNull
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
