package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @param <T> the event which is handled by the listener.
 * @author Terrarier2111
 * @since 1.0
 */
public interface Listener<T extends Event> {

    /**
     * This method is getting called each time the event
     * passed as the generic type parameter gets called.
     *
     * @param value the event which is called.
     */
    void trigger(@AssumeNotNull T value);

}
