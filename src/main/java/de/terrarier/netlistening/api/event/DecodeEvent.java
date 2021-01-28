package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataContainer;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a packet gets decoded.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class DecodeEvent extends ConnectionEvent {

	private final DataContainer data;
	
	public DecodeEvent(@NotNull Connection connection, @NotNull DataContainer data) {
		super(connection);
		this.data = data;
	}

	/**
	 * @return the data which was decoded.
	 */
	@NotNull
	public DataContainer getData() {
		return data;
	}

}
