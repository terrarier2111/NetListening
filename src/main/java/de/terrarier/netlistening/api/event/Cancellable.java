package de.terrarier.netlistening.api.event;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public abstract class Cancellable {

	private boolean cancelled;

	Cancellable() {}

	/**
	 * Sets if the event is to be cancelled or not.
	 *
	 * @param cancelled if the event is to be cancelled.
	 */
	public final void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * @return whether the event is cancelled or not.
	 */
	public final boolean isCancelled() {
		return cancelled;
	}

}
