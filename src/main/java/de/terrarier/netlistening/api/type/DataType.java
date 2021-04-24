package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.*;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.UUID;

/**
 * @since 1.0
 * @author Terrarier2111
 * @param <T> the type which this DataType represents.
 */
public abstract class DataType<T> {
	
	public static final DataType<Boolean> BOOLEAN = new DataTypeBoolean();
	public static final DataType<Byte> BYTE = new DataTypeByte();
	public static final DataType<byte[]> BYTE_ARRAY = new DataTypeByteArray();
	public static final DataType<Character> CHAR = new DataTypeChar();
	public static final DataType<Integer> INT = new DataTypeInt();
	public static final DataType<Short> SHORT = new DataTypeShort();
	public static final DataType<Double> DOUBLE = new DataTypeDouble();
	public static final DataType<String> STRING = new DataTypeString();
	public static final DataType<Long> LONG = new DataTypeLong();
	public static final DataType<Object> OBJECT = new DataTypeObject();
	public static final DataType<UUID> UUID = new DataTypeUUID();
	public static final DataType<Float> FLOAT = new DataTypeFloat();

	private static final DataTypeInternalPayload INTERNAL_PAYLOAD = new DataTypeInternalPayload();
	private static final DataType<Void> ENCRYPT = new DataTypeEncrypt();
	private static final DataType<Void> HMAC = new DataTypeHmac();

	private final byte id;
	private final byte minSize;
	private final boolean published;

	protected DataType(byte id, byte minSize, boolean published) {
		this.id = id;
		this.minSize = minSize;
		this.published = published;
	}

	@ApiStatus.Internal
	public T read0(@AssumeNotNull PacketDataDecoder.DecoderContext context, @AssumeNotNull List<Object> out,
				   @AssumeNotNull ByteBuf buffer) throws Exception {
		checkReadable(buffer, minSize);
		return read(context.getApplication(), context.getConnection(), buffer);
	}

	@ApiStatus.Internal
	protected abstract T read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
							  @AssumeNotNull ByteBuf buffer) throws CancelSignal;

	@ApiStatus.Internal
	public void write0(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, T data) throws CancelSignal {
		checkWriteable(application, buffer, minSize);
		write(application, buffer, data);
	}

	@ApiStatus.Internal
	protected abstract void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, T data)
			throws CancelSignal;
	
	public final byte getId() {
		return id;
	}

	@ApiStatus.Internal
	public final boolean isPublished() {
		return published;
	}

	public final int getMinSize() {
		return minSize;
	}

	@Deprecated
	@AssumeNotNull
	public final DataComponent<T> newComponent(T content) {
		return new DataComponent<>(this, content);
	}

	@ApiStatus.Internal
	@SuppressWarnings("unchecked")
	public final void writeUnchecked(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buf, @AssumeNotNull Object data)
			throws CancelSignal {
		write0(application, buf, (T) data);
	}

	@ApiStatus.Internal
	protected static void checkReadable(@AssumeNotNull ByteBuf buffer, int length) throws CancelReadSignal {
		if (buffer.readableBytes() < length) {
			throw new CancelReadSignal(length);
		}
	}

	@ApiStatus.Internal
	static void checkWriteable(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, int length) {
		ByteBufUtilExtension.correctSize(buffer, length, application.getBuffer());
	}

	@ApiStatus.Internal
	@AssumeNotNull
	public static DataType<?> fromId(byte id) {
			switch (id) {
				case 0x1:
					return BOOLEAN;
				case 0x2:
					return BYTE;
				case 0x3:
					return BYTE_ARRAY;
				case 0x4:
					return CHAR;
				case 0x5:
					return INT;
				case 0x6:
					return SHORT;
				case 0x7:
					return STRING;
				case 0x8:
					return OBJECT;
				case 0x9:
					return LONG;
				case 0xA:
					return DOUBLE;
				case 0xB:
					return UUID;
				case 0xC:
					return FLOAT;
				default:
					throw new IllegalArgumentException("Tried to resolve a data type with an invalid id! ("
							+ Integer.toHexString(id) + ')');
			}
	}

	@ApiStatus.Internal
	@AssumeNotNull
	public static DataTypeInternalPayload getDTIP() {
		return INTERNAL_PAYLOAD;
	}

	@ApiStatus.Internal
	@AssumeNotNull
	public static DataType<Void> getDTE() {
		return ENCRYPT;
	}

	@ApiStatus.Internal
	@AssumeNotNull
	public static DataType<Void> getDTHMAC() {
		return HMAC;
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		return obj != null && getClass() == obj.getClass();
	}

	/**
	 * @see Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		return id;
	}
	
}
