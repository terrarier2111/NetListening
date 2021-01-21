package de.terrarier.netlistening.internals;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class CancelReadingSignal extends Exception {

	public final int size;
	public final boolean array;
	
	public CancelReadingSignal(int size) {
		this(size, false);
	}
	
	public CancelReadingSignal(int size, boolean array) {
		this.size = size;
		this.array = array;
	}

	@NotNull
	@Override
	public Throwable initCause(@NotNull Throwable cause) {
		return this;
	}

	@NotNull
	@Override
	public Throwable fillInStackTrace() {
		return this;
	}

}
