package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event gets called when an exception is thrown.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class ExceptionTrowEvent implements Event {

    private boolean print = true;
    private final Throwable exception;

    @ApiStatus.Internal
    public ExceptionTrowEvent(@AssumeNotNull Throwable exception) {
        this.exception = exception;
    }

    /**
     * @return whether the stacktrace should be printed or not.
     */
    public boolean isPrint() {
        return print;
    }

    /**
     * @return the exception which got thrown.
     */
    @AssumeNotNull
    public Throwable getException() {
        return exception;
    }

    /**
     * Sets whether the stacktrace should be print or not.
     *
     * @param print whether the stacktrace should be print or not.
     */
    public void setPrint(boolean print) {
        this.print = print;
    }

}
