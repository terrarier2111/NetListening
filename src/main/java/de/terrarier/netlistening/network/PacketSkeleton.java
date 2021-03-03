package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.type.DataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class PacketSkeleton {

	private final int id;
	private final DataType<?>[] data;
	private final int hash;
	private volatile boolean registered;
	
	public PacketSkeleton(int id, @NotNull DataType<?>... data) {
		this.id = id;
		this.data = data;
		this.hash = Arrays.hashCode(data);
	}
	
	public int getId() {
		return id;
	}

	@NotNull
	public DataType<?>[] getData() {
		return data;
	}

	protected boolean isRegistered() {
		return registered;
	}

	public void register() {
		registered = true;
	}

	@Override
	public int hashCode() {
		return hash;
	}
}
