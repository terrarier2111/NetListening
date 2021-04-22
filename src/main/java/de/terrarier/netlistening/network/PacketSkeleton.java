package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class PacketSkeleton {

	private final int id;
	private final DataType<?>[] data;
	private final int hash;
	private volatile boolean registered;
	
	PacketSkeleton(int id, @AssumeNotNull DataType<?>... data) {
		this.id = id;
		this.data = data;
		hash = Arrays.hashCode(data);
	}
	
	public int getId() {
		return id;
	}

	@AssumeNotNull
	public DataType<?>[] getData() {
		return data;
	}

	boolean isRegistered() {
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
