package de.terrarier.netlistening;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.event.Listener;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.Set;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public interface Application {

	String DECODER = "nl_decoder";
	String ENCODER = "nl_encoder";
	String TIMEOUT_HANDLER = "nl_timeout_handler";

	/**
	 * @return the encoding used to encode String which are being sent through the network.
	 */
	@ApiStatus.Internal
	@NotNull
	Charset getStringEncoding();

	/**
	 * Registers a listener which can be used to perform an action chosen by the user
	 * when a certain event happens.
	 *
	 * @param listener the listener which should be registered.
	 */
	void registerListener(@NotNull Listener<?> listener);

	/**
	 * Unregisters all listeners of a specific type.
	 *
	 * @param listenerType the type of the listeners which should be unregistered.
	 */
	void unregisterListeners(@NotNull ListenerType listenerType);

	/**
	 * Maps a specific channel to the connection wrapping the specified channel.
	 *
	 * @param channel the channel which underlies the connection.
	 * @return the connection which wraps the passed channel and
	 * if not available, null.
	 */
	Connection getConnection(Channel channel);

	/**
	 * Maps a specific id to the connection being identified with this id.
	 *
	 * @param id the id of the requested connection.
	 * @return the connection which is identified with the passed id and
	 * if not available, null.
	 */
	Connection getConnection(int id);

	/**
	 * @return a list of all connections.
	 */
	@NotNull
	Set<Connection> getConnections();

	/**
	 * Stops the application and closes all connections.
	 */
	void stop();

	/**
	 * Disconnects a specific connection.
	 *
	 * @param connection the connection which should be disconnected.
	 */
	void disconnect(Connection connection);

	/**
	 * @see Application#sendData(Connection, DataContainer) 
	 */
	@Deprecated
	default void sendData(@NotNull DataContainer data, Connection connection) {
		sendData(connection, data);
	}

	/**
	 * Sends data to a specific connection.
	 *
	 * @param data the data which gets sent.
	 * @param connection the connection the data gets sent to.
	 * @deprecated use @link { Connection#sendData(DataContainer) } instead.
	 */
	@Deprecated
	void sendData(Connection connection, @NotNull DataContainer data);

	/**
	 * Sends data to a specific connection.
	 *
	 * @param data the data which gets sent.
	 * @param connection the connection the data gets sent to.
	 * @deprecated use @link { Connection#sendData(DataComponent) } instead.
	 */
	@Deprecated
	void sendData(@NotNull DataComponent<?> data, Connection connection);

	/**
	 * Sends data to all connection.
	 *
	 * @param data the data which gets sent.
	 */
	void sendData(@NotNull DataContainer data);

	/**
	 * Sends data to all connections.
	 *
	 * @param data the data which gets sent.
	 */
	default void sendData(@NotNull Object... data) {
		sendData(false, data);
	}

	/**
	 * Sends data to all connections.
	 *
	 * @param encrypted if the traffic is to be encrypted.
	 * @param data the data which gets sent.
	 */
	default void sendData(boolean encrypted, @NotNull Object... data) {
		if(data.length == 0) {
			throw new IllegalArgumentException("Please pass the data which is to be sent, you may not send empty arrays.");
		}

		final DataContainer container = new DataContainer();
		container.addAll(data);
		container.setEncrypted(encrypted);
		sendData(container);
	}

	/**
	 * Sends data to all connection.
	 *
	 * @param data the data which gets sent.
	 */
	@Deprecated
	default void sendData(@NotNull DataComponent<?> data) {
		final DataContainer container = new DataContainer();
		container.addComponent(data);
		sendData(container);
	}

	abstract class Builder<A extends Application, B extends Builder<A, B>> {

		/**
		 * Sets a specific read timeout for the connection, and automatically writes
		 * data to the other end of connections every {@code (timeout / 2)} milliseconds.
		 *
		 * @param timeout the amount of milliseconds in which any data should be received.
		 * @return the local reference.
		 */
		@NotNull
		public abstract B timeout(long timeout);

		/**
		 * Sets the buffer size which is added on top of the required space,
		 * every time a buffer is expanded.
		 *
		 * @param buffer the additional size added to the buffer.
		 * @return the local reference.
		 */
		@NotNull
		public abstract B buffer(int buffer);

		/**
		 * Sets a specific option of the channel to a specific
		 * value when the channel gets opened!
		 *
		 * @param option the option to be set.
		 * @param value the value to be assigned to the option.
		 * @param <T> the type of the option.
		 * @return the local reference.
		 */
		@NotNull
		public abstract <T> B option(@NotNull ChannelOption<T> option, T value);

		/**
		 * Sets the serialization provider which is to be used to
		 * perform serialization operations.
		 *
		 * @param serializationProvider the serialization provider which provides
		 * an implementation for serialization operations.
		 * @return the local reference.
		 */
		@NotNull
		public abstract B serialization(@NotNull SerializationProvider serializationProvider);

		/**
		 * Builds the application, sets its default values and starts it.
		 *
		 * @return the started application.
		 */
		@NotNull
		public abstract A build();

	}
	
}
