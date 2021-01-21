package de.terrarier.netlistening.network;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum PacketSynchronization {

	NONE, SIMPLE;

	private static final PacketSynchronization[] VALUES = values();

	@NotNull
	public static PacketSynchronization fromId(byte id) {
		return VALUES[id];
	}

}
