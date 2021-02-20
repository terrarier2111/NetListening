package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.internals.*;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
	public static final DataType<java.util.UUID> UUID = new DataTypeUUID();
	public static final DataType<Float> FLOAT = new DataTypeFloat();

	private static final DataType<InternalPayload> INTERNAL_PAYLOAD = new DataTypeInternalPayload();
	private static final DataType<Void> ENCRYPT = new DataTypeEncrypt();
	private static final DataType<Void> HMAC = new DataTypeHmac();

	private final byte id;
	private final byte minSize;
	private final boolean published;

	public DataType(byte id, byte minSize, boolean published) {
		this.id = id;
		this.minSize = minSize;
		this.published = published;
	}
	
	public T read0(@NotNull ChannelHandlerContext ctx, @NotNull List<Object> out, @NotNull Application application,
				   @NotNull ByteBuf buffer) throws Exception {
		checkReadable(buffer, minSize);
		return read(application, ctx.channel(), buffer);
	}

	protected abstract T read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
			throws CancelReadingSignal;

	public void write0(@NotNull Application application, @NotNull ByteBuf buffer, T data) {
		checkWriteable(application, buffer, minSize);
		write(application, buffer, data);
	}

	protected abstract void write(@NotNull Application application, @NotNull ByteBuf buffer, T data);
	
	public final byte getId() {
		return id;
	}
	
	public final boolean isPublished() {
		return published;
	}

	@Deprecated
	public final int getMinSize() {
		return minSize;
	}

	@Deprecated
	@NotNull
	public final DataComponent<T> newComponent(T content) {
		return new DataComponent<>(this, content);
	}
	
	@SuppressWarnings("unchecked")
	public final void writeUnchecked(@NotNull Application application, @NotNull ByteBuf buf, @NotNull Object data) {
		write0(application, buf, (T) data);
	}

	protected static void checkReadable(@NotNull ByteBuf buffer, int length) throws CancelReadingSignal {
		checkReadable(buffer, length, false);
	}

	protected static void checkReadable(@NotNull ByteBuf buffer, int length, boolean array) throws CancelReadingSignal {
		if (buffer.readableBytes() < length) {
			throw new CancelReadingSignal(length, array);
		}
	}

	protected static void checkWriteable(@NotNull Application application, @NotNull ByteBuf buffer, int length) {
		ByteBufUtilExtension.correctSize(buffer, length, application.getBuffer());
	}

	// TODO: Check if an array instead of this switch case would be faster!
	@NotNull
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
			}
		throw new IllegalArgumentException("Tried to resolve a data type with an invalid id! (" + Integer.toHexString(id) + ")");
	}

	@NotNull
	public static DataType<InternalPayload> getDTIP() {
		return INTERNAL_PAYLOAD;
	}

	@NotNull
	public static DataType<Void> getDTE() {
		return ENCRYPT;
	}

	@NotNull
	public static DataType<Void> getDTHMAC() {
		return HMAC;
	}

	@Override
	public final boolean equals(Object obj) {
		return obj != null && getClass() == obj.getClass();
	}

	@Override
	public final int hashCode() {
		return id;
	}
	
}
