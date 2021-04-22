package de.terrarier.netlistening.internals;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public class CancelSignal extends Exception {

	public static final CancelSignal INSTANCE = new CancelSignal();

	CancelSignal() {}

	@AssumeNotNull
	@Override
	public Throwable initCause(Throwable cause) {
		return this;
	}

	@AssumeNotNull
	@Override
	public Throwable fillInStackTrace() {
		return this;
	}

}
