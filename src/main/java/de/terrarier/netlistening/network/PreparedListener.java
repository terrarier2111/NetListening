package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.Type;
import de.terrarier.netlistening.api.event.DecodeListener;
import de.terrarier.netlistening.api.event.Event;
import de.terrarier.netlistening.api.event.PacketListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

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
	private Type[] types;
	private final int hash;
	
	public PreparedListener(@NotNull DecodeListener listener) {
		wrapped = listener;
		try {
			final Method method = listener.getClass().getDeclaredMethod("trigger", Event.class);
			final PacketListener packetListener = method.getAnnotation(PacketListener.class);
			if(packetListener != null) {
				types = packetListener.dataTypes();
			}else {
				types = EMPTY_TYPES;
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace(); // TODO: Check if there's a better solution cuz this should never occur!
		}
		hash = Arrays.hashCode(types);
	}

	@NotNull
	public DecodeListener getWrapped() {
		return wrapped;
	}

	@NotNull
	public Type[] getTypes() {
		return types;
	}

	@Override
	public int hashCode() {
		return hash;
	}
}
