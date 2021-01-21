package de.terrarier.netlistening.api.event;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public abstract class Cancellable {

	private boolean cancelled;

	/**
	 * Sets if the event is to be cancelled or not.
	 *
	 * @param cancelled if the event is to be cancelled.
	 */
	public final void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * @return if the event is canceled.
	 */
	public final boolean isCancelled() {
		return cancelled;
	}

}
