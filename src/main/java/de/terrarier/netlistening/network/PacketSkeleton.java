package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.type.DataType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class PacketSkeleton {
	
	private final DataType<?>[] data;
	private final int id;
	private volatile boolean registered;
	
	public PacketSkeleton(int id, @NotNull DataType<?>... data) {
		this.data = data;
		this.id = id;
	}

	@NotNull
	public DataType<?>[] getData() {
		return data;
	}
	
	protected int getId() {
		return id;
	}

	protected boolean isRegistered() {
		return registered;
	}

	public void register() {
		registered = true;
	}

}
