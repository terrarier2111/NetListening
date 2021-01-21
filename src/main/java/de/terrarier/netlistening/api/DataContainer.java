package de.terrarier.netlistening.api;

import de.terrarier.netlistening.api.type.DataType;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataContainer {

	private final List<DataComponent<?>> data;
	private int readerIndex;
	private boolean encrypted;

	public DataContainer() {
		data = new ArrayList<>();
	}

	public DataContainer(@NotNull DataComponent<?>... components) {
		data = new ArrayList<>(Arrays.asList(components)); // TODO: Check if manually adding is better in terms of performance!
	}
	
	public DataContainer(@NotNull List<DataComponent<?>> components) {
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
	public void addComponent(@NotNull DataComponent<?> component) {
		data.add(component);
	}

	/**
	 * Converts the input to a data component and adds it to the container.
	 *
	 * @param data the data to add.
	 */
	@SuppressWarnings("unchecked")
	public void add(@NotNull Object data) {
		DataType<?> type;

		if(data instanceof String) {
			type = DataType.STRING;
		}else if(data instanceof Integer) {
			type = DataType.INT;
		}else if(data.getClass() == byte[].class) {
			type = DataType.BYTE_ARRAY;
		}else if(data instanceof Long) {
			type = DataType.LONG;
		}else if(data instanceof Boolean) {
			type = DataType.BOOLEAN;
		}else if(data instanceof Double) {
			type = DataType.DOUBLE;
		}else if(data instanceof Byte) {
			type = DataType.BYTE;
		}else if(data instanceof Short) {
			type = DataType.SHORT;
		}else if(data instanceof Float) {
			type = DataType.FLOAT;
		}else if(data instanceof UUID) {
			type = DataType.UUID;
		}else if(data instanceof Character) {
			type = DataType.CHAR;
		}else {
			type = DataType.OBJECT;
		}

		addComponent(new DataComponent(type, data));
	}

	/**
	 * Adds an array of objects as data components to the container.
	 *
	 * @param data the data to add.
	 */
	public void addAll(@NotNull Object... data) {
		if(data == null || data.length == 0) {
			return;
		}
		for(Object comp : data) {
			add(comp);
		}
	}

	/**
	 * @return a list containing the data contained in the container.
	 */
	@NotNull
	public List<DataComponent<?>> getData() {
		return data;
	}

	/**
	 * Writes the contained data to the passed channel.
	 *
	 * @param channel the channel the data should be written to.
	 */
	public void write(@NotNull Channel channel) {
		channel.writeAndFlush(this);
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
		while(true) {
			if(!passThroughPart(data)) {
				return;
			}
		}
	}

	/**
	 * @return the next object available at the
	 * readerIndex, if no object is available, null.
	 */
	public <T> T read() {
		final DataComponent<T> component = readRaw();
		if(component == null) {
			return null;
		}
		return component.getData();
	}

	/**
	 * @return the next component available at index
	 * readerIndex, if no component is available null.
	 */
	@SuppressWarnings("unchecked")
	public <T> DataComponent<T> readRaw() {
		if(!isReadable()) {
			return null;
		}
		return (DataComponent<T>) data.get(readerIndex++);
	}

	/**
	 * @return an array containing all content of the DataContainer which
	 * wasn't read yet.
	 */
	@NotNull
	public Object[] readRemaining() {
		final int remainingReads = remainingReads();
		final Object[] ret = new Object[remainingReads];
		for(int i = 0; i < remainingReads; i++) {
			ret[i] = read();
		}
		return ret;
	}

	/**
	 * @param index the index specifying which element is to be returned.
	 * @return the element in the DataContainer at the index passed as the parameter.
	 */
	@SuppressWarnings("unchecked")
	@NotNull
	public <T> DataComponent<T> get(int index) {
		return (DataComponent<T>) data.get(index);
	}

	/**
	 * @return whether it can be read from the DataContainer or not.
	 */
	public boolean isReadable() {
		return readerIndex < data.size();
	}

	/**
	 * @return the amount of read calls which can be
	 * performed returning an element contained by this DataContainer.
	 */
	public int remainingReads() {
		return data.size() - readerIndex;
	}

	/**
	 * Resets the readerIndex so that the next read call will
	 * return the first component once again.
	 */
	public void resetReaderIndex() {
		readerIndex = 0;
	}

	/**
	 * Sets whether the data contained in this container gets
	 * encrypted.
	 *
	 * @param encrypted if the data contained gets encrypted.
	 */
	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	/**
	 * @return whether the data contained in this DataContainer gets encrypted
	 * when it is being sent through the network or not.
	 */
	public boolean isEncrypted() {
		return encrypted;
	}

	/**
	 * @see Object
	 */
	@NotNull
	@Override
	public String toString() {
		return "DataContainer: Length: " + data.size() + " ReaderIndex: " + readerIndex;
	}

}
