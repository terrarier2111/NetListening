package de.terrarier.netlistening.api.event;

/**
 * This listener can be used to detect MITM attacks.
 * It is only called on the client side.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public interface KeyChangeListener extends Listener<KeyChangeEvent> {}
