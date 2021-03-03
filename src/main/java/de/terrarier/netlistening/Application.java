package de.terrarier.netlistening;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.compression.CompressionSetting;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.event.Listener;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.network.PacketCache;
import de.terrarier.netlistening.network.PacketSynchronization;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
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
	String TIMEOUT_HANDLER = "timeOutHandler";

	/**
	 * @return if the application is a client!
	 */
	boolean isClient();

	/**
	 * @return the caching mode used to cache packets.
	 */
	@NotNull
	PacketCaching getCaching();

	/**
	 * @return the synchronization mode used to synchronize packet ids through multiple connections.
	 */
	@NotNull
	PacketSynchronization getPacketSynchronization();

	/**
	 * @return the encoding used to encode String which are being sent through the network.
	 */
	@NotNull
	Charset getStringEncoding();

	/**
	 * @return the packet cache used by the application to map packet ids
	 * to packet content.
	 */
	@NotNull
	PacketCache getCache();

	/**
	 * @return the buffer size which is added on top of the required space,
	 * every time a buffer is expanded.
	 */
	int getBuffer();

	/**
	 * @return the encryption settings which should be used to encrypt traffic
	 * which was marked to encrypt.
	 */
	EncryptionSetting getEncryptionSetting();

	/**
	 * @return the compression setting containing information
	 * about which compression techniques should be applied on
	 * specific data.
	 */
	@NotNull
	CompressionSetting getCompressionSetting();

	/**
	 * @return the serialization provider which handles the serialization
	 * of specific data.
	 */
	@NotNull
	SerializationProvider getSerializationProvider();

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
	 * @return a list of all active connections.
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
	 * Sends data to a connection with a specific id.
	 *
	 * @param data the data which gets sent.
	 * @param connection the connection the data gets sent to.
	 */
	void sendData(@NotNull DataContainer data, Connection connection);

	/**
	 * Sends data to a connection with a specific id.
	 *
	 * @param data the data which gets sent.
	 * @param connection the connection the data gets sent to.
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
	 * Sends data to all connection.
	 *
	 * @param data the data which gets sent.
	 */
	@Deprecated
	void sendData(@NotNull DataComponent<?> data);

	abstract class Builder<A extends Application, B extends Builder<A, B>> {

		/**
		 * Sets a specific read timeout for the connection, and automatically writes
		 * data to the other end of connections every timeout / 2 milliseconds.
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
