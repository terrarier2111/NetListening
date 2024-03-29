/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.event.Listener;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.serialization.SerializationProvider;
import de.terrarier.netlistening.internal.AssumeNotNull;
import io.netty.channel.ChannelOption;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.Set;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public interface Application {

    String DECODER = "nl_decoder";
    String ENCODER = "nl_encoder";
    String TIMEOUT_HANDLER = "nl_timeout_handler";

    /**
     * @return the encoding used to encode String which are being sent through the network.
     */
    @ApiStatus.Internal
    @AssumeNotNull
    Charset getStringEncoding();

    /**
     * Registers a listener which can be used to perform an action chosen by the user
     * when a certain event happens.
     *
     * @param listener the listener which should be registered.
     * @return an id which the listener can be identified by.
     */
    long registerListener(@NotNull Listener<?> listener);

    /**
     * Unregisters all listeners of a specific type.
     *
     * @param listenerType the type of the listeners which should be unregistered.
     */
    @ApiStatus.Experimental
    void unregisterListeners(@NotNull ListenerType listenerType);

    /**
     * Unregisters a specific listener which is identified by
     * the return value of {@link Application#registerListener(Listener)}.
     *
     * @param listenerId the id the specific listener is identified by.
     */
    void unregisterListener(long listenerId);

    /**
     * @return a list of all connections.
     */
    @AssumeNotNull
    Set<Connection> getConnections();

    /**
     * Stops the application and closes all connections.
     */
    void stop();

    /**
     * Sends data to all connections.
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
        if (data.length == 0) {
            throw new IllegalArgumentException("Please pass the data which is to be sent, you may not send empty arrays.");
        }

        final DataContainer container = new DataContainer();
        container.addAll(data);
        sendData(container);
    }

    /**
     * Sends data to all connections.
     *
     * @param encrypted if the traffic is to be encrypted.
     * @param data      the data which gets sent.
     * @throws IllegalArgumentException if the passed object array is empty.
     * @deprecated use data containers directly instead because this method is ambiguous most of the time,
     * use {@link Application#sendDataEncrypted(Object...)) instead!
     */
    @Deprecated
    default void sendData(boolean encrypted, @NotNull Object... data) {
        if (data.length == 0) {
            throw new IllegalArgumentException("Please pass the data which is to be sent, you may not send empty arrays.");
        }

        final DataContainer container = new DataContainer();
        container.addAll(data);
        container.setEncrypted(encrypted);
        sendData(container);
    }

    /**
     * Encrypts and sends data to all connections.
     *
     * @param data the data which gets encrypted and sent.
     */
    default void sendDataEncrypted(@NotNull Object... data) {
        if (data.length == 0) {
            throw new IllegalArgumentException("Please pass the data which is to be sent, you may not send empty arrays.");
        }

        final DataContainer container = new DataContainer();
        container.addAll(data);
        container.setEncrypted(true);
        sendData(container);
    }

    /**
     * Sends data to all connection.
     *
     * @param data the data which gets sent.
     * @deprecated use {@link Application#sendData(DataContainer)} directly instead!
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
        @AssumeNotNull
        public abstract B timeout(long timeout);

        /**
         * Sets the buffer size which is added on top of the required space,
         * every time a buffer is expanded.
         *
         * @param buffer the additional size added to the buffer.
         * @return the local reference.
         */
        @AssumeNotNull
        public abstract B buffer(int buffer);

        /**
         * Sets a specific option of the channel to a specific
         * value when the channel gets opened!
         *
         * @param option the option to be set.
         * @param value  the value to be assigned to the option.
         * @param <T>    the type of the option.
         * @return the local reference.
         */
        @AssumeNotNull
        public abstract <T> B option(@NotNull ChannelOption<T> option, T value);

        /**
         * Sets the serialization provider which is to be used to
         * perform serialization operations.
         *
         * @param serializationProvider the serialization provider which provides
         *                              an implementation for serialization operations.
         * @return the local reference.
         */
        @AssumeNotNull
        public abstract B serialization(@NotNull SerializationProvider serializationProvider);

        /**
         * Builds the application, sets its default values and starts it.
         *
         * @return the started application.
         */
        @AssumeNotNull
        public abstract A build();

    }

}
