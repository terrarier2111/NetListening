package de.terrarier.netlistening.api.event;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 * @param <T>
 */
public interface Listener<T> {

	/**
	 * This method is getting called each time the event
	 * passed as the generic type parameter gets called
	 *
	 * @param value the event which is called.
	 */
	void trigger(@NotNull T value);
	
}
