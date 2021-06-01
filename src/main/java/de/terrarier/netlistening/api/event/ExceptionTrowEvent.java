/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event gets called when an exception is thrown.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class ExceptionTrowEvent implements Event {

    private final Throwable thrown;
    private boolean print = true;

    @ApiStatus.Internal
    public ExceptionTrowEvent(@AssumeNotNull Throwable thrown) {
        this.thrown = thrown;
    }

    /**
     * @deprecated use {@link ExceptionTrowEvent#getThrown()} instead!
     */
    @Deprecated
    @AssumeNotNull
    public Throwable getException() {
        return thrown;
    }

    /**
     * @return the throwable which got thrown.
     */
    @AssumeNotNull
    public Throwable getThrown() {
        return thrown;
    }

    /**
     * Sets whether the stacktrace should be printed or not.
     *
     * @param print whether the stacktrace should be print or not.
     */
    public void setPrint(boolean print) {
        this.print = print;
    }

    /**
     * @return whether the stacktrace should be printed or not.
     */
    public boolean isPrint() {
        return print;
    }

}
