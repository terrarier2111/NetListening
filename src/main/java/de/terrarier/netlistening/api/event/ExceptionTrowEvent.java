package de.terrarier.netlistening.api.event;

import org.jetbrains.annotations.NotNull;

/**
 * This event gets called when a exception is thrown.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class ExceptionTrowEvent implements Event {

    private boolean print = true;
    private final Throwable exception;

    public ExceptionTrowEvent(@NotNull Throwable exception) {
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
    @NotNull
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
