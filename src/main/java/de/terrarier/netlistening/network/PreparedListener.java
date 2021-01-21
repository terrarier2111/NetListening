package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.Type;
import de.terrarier.netlistening.api.event.DecodeListener;
import de.terrarier.netlistening.api.event.PacketListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class PreparedListener {

	private static final Type[] EMPTY_TYPES = new Type[0];
	private final DecodeListener wrapped;
	private Type[] types;
	
	public PreparedListener(@NotNull DecodeListener listener) {
		wrapped = listener;
		try {
			final Method method = listener.getClass().getDeclaredMethod("trigger", Object.class);
			final PacketListener packetListener = method.getAnnotation(PacketListener.class);
			if(packetListener != null) {
				types = packetListener.dataTypes();
			}else {
				types = EMPTY_TYPES;
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace(); // TODO: Check if there's a better solution cuz this should never occur!
		}
	}

	@NotNull
	public DecodeListener getWrapped() {
		return wrapped;
	}

	@NotNull
	public Type[] getTypes() {
		return types;
	}

}
