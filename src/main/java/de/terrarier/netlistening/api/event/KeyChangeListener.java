package de.terrarier.netlistening.api.event;

/**
 * This listener can be used to detect MITM attacks.
 * It is only called on the client side.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public interface KeyChangeListener extends Listener<KeyChangeEvent> {}
