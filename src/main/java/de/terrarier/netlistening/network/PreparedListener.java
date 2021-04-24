package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.Type;
import de.terrarier.netlistening.api.event.DecodeListener;
import de.terrarier.netlistening.api.event.Event;
import de.terrarier.netlistening.api.event.PacketListener;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class PreparedListener {

	private static final Type[] EMPTY_TYPES = new Type[0];
	private final DecodeListener wrapped;
	private final Type[] types;
	private final int hash;
	
	public PreparedListener(@AssumeNotNull DecodeListener listener) throws NoSuchMethodException, SecurityException {
		wrapped = listener;
		final Method method = listener.getClass().getDeclaredMethod("trigger", Event.class);
		final PacketListener packetListener = method.getAnnotation(PacketListener.class);
		types = packetListener != null ? packetListener.dataTypes() : EMPTY_TYPES;
		hash = Arrays.hashCode(types);
	}

	@AssumeNotNull
	public DecodeListener getWrapped() {
		return wrapped;
	}

	@AssumeNotNull
	public Type[] getTypes() {
		return types;
	}

	/**
	 * @see Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hash;
	}

}
