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
package de.terrarier.netlistening.api;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.internal.AssumeNotNull;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.terrarier.netlistening.api.type.DataType.*;
import static de.terrarier.netlistening.util.ObjectUtilFallback.checkPositiveOrZero;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class DataContainer {

    private final List<DataComponent<?>> data;
    private int readerIndex;
    private boolean encrypted;

    public DataContainer() {
        data = new ArrayList<>();
    }

    @ApiStatus.Internal
    public DataContainer(@AssumeNotNull List<DataComponent<?>> components) {
        data = components;
    }

    /**
     * @return the amount of data components contained in the container.
     */
    public int getSize() {
        return data.size();
    }

    /**
     * Adds a component to the container.
     *
     * @param component the component which should be added.
     */
    @ApiStatus.Internal
    public void addComponent(@AssumeNotNull DataComponent<?> component) {
        data.add(component);
    }

    /**
     * Converts the input to a data component and adds it to the container.
     *
     * @param data the data to add.
     */
    @SuppressWarnings("unchecked")
    public void add(@NotNull Object data) {
        final DataType<?> type;

        if (data instanceof String) {
            type = STRING;
        } else if (data instanceof Integer) {
            type = INT;
        } else if (data.getClass() == byte[].class) {
            type = BYTE_ARRAY;
        } else if (data instanceof Long) {
            type = LONG;
        } else if (data instanceof Boolean) {
            type = BOOLEAN;
        } else if (data instanceof Double) {
            type = DOUBLE;
        } else if (data instanceof Byte) {
            type = BYTE;
        } else if (data instanceof Float) {
            type = FLOAT;
        } else if (data instanceof Short) {
            type = SHORT;
        } else if (data instanceof UUID) {
            type = UUID;
        } else if (data instanceof Character) {
            type = CHAR;
        } else {
            type = OBJECT;
        }

        addComponent(new DataComponent(type, data));
    }

    /**
     * Adds an array of objects as data components to the container.
     *
     * @param data the data to add.
     */
    public void addAll(@NotNull Object... data) {
        if (data == null || data.length == 0) {
            return;
        }
        for (Object comp : data) {
            add(comp);
        }
    }

    /**
     * @return a list containing the data contained in this container.
     */
    @AssumeNotNull
    @ApiStatus.Internal
    public List<DataComponent<?>> getData() {
        return data;
    }

    /**
     * Writes the contained data to the passed channel.
     *
     * @param channel the channel the data should be written to.
     */
    @Deprecated
    public void write(@NotNull Channel channel) {
        channel.writeAndFlush(this, channel.voidPromise());
    }

    /**
     * Passes the first readable part contained in the
     * passed data container to this data container.
     *
     * @param data the data container the data should be read from.
     */
    public boolean passThroughPart(@NotNull DataContainer data) {
        final DataComponent<?> component = data.readRaw();
        if (component == null) {
            return false;
        }
        this.data.add(component);
        return true;
    }

    /**
     * Adds all data contained in the passed
     * DataContainer into the current one.
     *
     * @param data the DataContainer containing the data which gets added.
     */
    public void passThrough(@NotNull DataContainer data) {
        while (passThroughPart(data));
    }

    /**
     * Creates a sub container which consists of all elements
     * from {@code startIndex} to {@code data#size}.
     *
     * @param startIndex the index from which the sub container should be filled.
     * @return the created sub container.
     */
    @AssumeNotNull
    public DataContainer subContainer(int startIndex) {
        return subContainer(startIndex, data.size());
    }

    /**
     * Creates a sub container which consists of all elements
     * from {@code startIndex} to {@code endIndex}.
     *
     * @param startIndex the index from which the sub container should be filled.
     * @param endIndex   the index up to which the sub container should be filled.
     * @return the created sub container.
     * @throws IllegalArgumentException if {@code startIndex} is greater than {@code endIndex}.
     */
    @AssumeNotNull
    public DataContainer subContainer(int startIndex, int endIndex) {
        if (checkPositiveOrZero(startIndex, "startIndex") > endIndex) {
            throw new IllegalArgumentException("startIndex has to be smaller than endIndex!");
        }

        final DataContainer ret = new DataContainer();
        for (int i = startIndex; i < endIndex; i++) {
            ret.data.add(data.get(i));
        }
        return ret;
    }

    /**
     * @return the next object available at the
     * readerIndex, if no object is available, null.
     */
    public <T> T read() {
        final DataComponent<T> component = readRaw();
        if (component == null) {
            return null;
        }
        return component.getData();
    }

    /**
     * @return the next component available at index
     * {@code readerIndex}, if no component is available, null.
     */
    @SuppressWarnings("unchecked")
    @ApiStatus.Internal
    public <T> DataComponent<T> readRaw() {
        if (!isReadable()) {
            return null;
        }
        return (DataComponent<T>) data.get(readerIndex++);
    }

    /**
     * @return an array containing the data container's content which
     * wasn't read yet.
     */
    @AssumeNotNull
    public Object[] readRemaining() {
        final int remainingReads = remainingReads();
        final Object[] ret = new Object[remainingReads];
        for (int i = 0; i < remainingReads; i++) {
            ret[i] = data.get(readerIndex++).getData();
        }
        return ret;
    }

    /**
     * @return whether it can be read from the data container or not.
     */
    public boolean isReadable() {
        return readerIndex < data.size();
    }

    /**
     * @return the amount of read calls which can be performed
     * returning an element contained by this DataContainer.
     */
    public int remainingReads() {
        return data.size() - readerIndex;
    }

    /**
     * Resets the {@code readerIndex} so that the next read call will
     * return the first component once again.
     */
    public void resetReaderIndex() {
        readerIndex = 0;
    }

    /**
     * Increases the {@code readerIndex} by {@code elements}.
     *
     * @param elements the number of elements which should be skipped.
     * @throws IllegalArgumentException if {@code elements} is greater than {@code DataContainer#getSize()}.
     */
    public void skip(int elements) {
        final int result = readerIndex + checkPositiveOrZero(elements, "elements");
        if (result > data.size()) {
            throw new IllegalArgumentException("elements may not be > size");
        }
        readerIndex = result;
    }

    /**
     * Sets whether the data contained in this container gets
     * encrypted or not.
     *
     * @param encrypted if the data contained gets encrypted.
     */
    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    /**
     * @return whether the data contained in this data container gets encrypted
     * when it is sent through the network or not.
     */
    public boolean isEncrypted() {
        return encrypted;
    }

    /**
     * @see Object#toString()
     */
    @AssumeNotNull
    @Override
    public String toString() {
        return "Length: " + data.size() + " ReaderIndex: " + readerIndex;
    }

}
