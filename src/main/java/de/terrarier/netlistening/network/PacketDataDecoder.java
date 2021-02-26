package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.event.*;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.CancelReadingSignal;
import de.terrarier.netlistening.internals.DataTypeInternalPayload;
import de.terrarier.netlistening.internals.InternalUtil;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import de.terrarier.netlistening.utils.ConversionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.EmptyArrays;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class PacketDataDecoder extends ByteToMessageDecoder {

    private final ApplicationImpl application;
    private final DataHandler handler;
    private boolean framing;
    private ByteBuf holdingBuffer;
    private ArrayList<DataComponent<?>> storedData;
    private int index;
    private PacketSkeleton packet;
    private boolean hasId;
    private boolean release;

    public PacketDataDecoder(@NotNull ApplicationImpl application, @NotNull DataHandler handler) {
        this.application = application;
        this.handler = handler;
    }

    @Override
    public void decode(@NotNull ChannelHandlerContext ctx, @NotNull ByteBuf buffer, @NotNull List<Object> out) throws Exception {
        final int readable = buffer.readableBytes();
        if (framing) {
            // Framing
            final ByteBuf tmp = holdingBuffer;
            final int writable = tmp.writableBytes();
            final boolean block = writable > readable;
            final byte[] remaining = ByteBufUtilExtension.readBytes(buffer, block ? readable : writable);
            if (remaining.length != 0) { // check for an empty packet, should never occur
                tmp.writeBytes(remaining);
            }
            if (block) {
                return;
            }
            framing = false;
            boolean release = false;
            if (!hasId) {
                final boolean[] idReadValidator = new boolean[1];
                readPacket(ctx, buffer, out, tmp, idReadValidator);
                release = idReadValidator[0];
            } else {
                hasId = false;
                if (packet != null) {
                    read(ctx, out, storedData, buffer, packet, index, tmp);
                } else {
                    readPayLoad(tmp, ctx.channel());
                }
            }
            if (release || !framing || !tmp.equals(holdingBuffer)) {
                tmp.release();
                if (!framing) {
                    holdingBuffer = null;
                }
            }
            return;
        }

        if (readable == 0) {
            application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    (EventManager.EventProvider<InvalidDataEvent>) () -> {
                final Connection connection = application.getConnection(ctx.channel());
                return new InvalidDataEvent(connection, DataInvalidReason.EMPTY_PACKET, EmptyArrays.EMPTY_BYTES);
            });

            throw new IllegalStateException("Received an empty packet!");
        }
        readPacket(ctx, buffer, out, buffer, null);
    }

    private void readPacket(@NotNull ChannelHandlerContext ctx, @NotNull ByteBuf buffer, @NotNull List<Object> out,
                            @NotNull ByteBuf idBuffer, boolean[] packetIdReadValidator) throws Exception {
        int id;
        try {
            id = InternalUtil.readInt(application, idBuffer);
            if (packetIdReadValidator != null) {
                packetIdReadValidator[0] = true;
            }
        } catch (VarIntUtil.VarIntParseException varIntParseException) {
            // preparing framing of packet id
            holdingBuffer = Unpooled.buffer(varIntParseException.requiredBytes);
            transferRemaining(buffer);
            packet = null;
            hasId = false;
            return;
        }

        if (id == 0x1) {
            // Dropping the keep alive packet content
            buffer.readByte();
            return;
        }

        if (id == 0x2) {
            if (!application.isClient()) {
                application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                        (EventManager.EventProvider<InvalidDataEvent>) () -> {
                    final Connection connection = application.getConnection(ctx.channel());
                    final byte[] data = application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.toVarInt(0x2) : ConversionUtil.intToByteArray(0x2);
                    return new InvalidDataEvent(connection, DataInvalidReason.MALICIOUS_ACTION, data);
                });

                throw new IllegalStateException("Received malicious data! (0x2)");
            }

            ((ClientImpl) application).pushCachedData();
            if (buffer.isReadable()) {
                decode(ctx, buffer, out);
            }
            return;
        }

        if (!buffer.isReadable()) {
            application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    (EventManager.EventProvider<InvalidDataEvent>) () -> {
                final Connection connection = application.getConnection(ctx.channel());
                final byte[] data = application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.toVarInt(id) : ConversionUtil.intToByteArray(id);
                return new InvalidDataEvent(connection, DataInvalidReason.INCOMPLETE_PACKET, data);
            });

            throw new IllegalStateException(
                    "An error occurred while decoding - the packet to decode was empty! (skipping current packet with id: "
                            + Integer.toHexString(id) + ")");
        }

        if (id == 0x0) {
            readPayLoad(buffer, ctx.channel());
            return;
        }

        final ConnectionImpl connection = (ConnectionImpl) application.getConnection(ctx.channel());
        final PacketSkeleton packet = connection.getCache().getInPacketFromId(id);
        if (packet == null) {
            application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    (EventManager.EventProvider<InvalidDataEvent>) () -> {
                final byte[] data = application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.toVarInt(id) : ConversionUtil.intToByteArray(id);
                return new InvalidDataEvent(connection, DataInvalidReason.INVALID_ID, data);
            });

            throw new IllegalStateException(
                    "An error occurred while decoding - the packet to decode wasn't recognizable because it wasn't registered before! ("
                            + Integer.toHexString(id) + ")");
        }

        read(ctx, out, new ArrayList<>(packet.getData().length), buffer, packet, 0, null);
    }

    @SuppressWarnings("unchecked")
    private void read(@NotNull ChannelHandlerContext ctx, @NotNull List<Object> out, @NotNull ArrayList<DataComponent<?>> data,
                      @NotNull ByteBuf buffer, @NotNull PacketSkeleton packet, int index, ByteBuf framingBuffer) throws Exception {
        final DataType<?>[] dataTypes = packet.getData();
        final int length = dataTypes.length;
        boolean ignore = false;

        for (int i = index; i < length; i++) {
            final DataType<?> dataType = dataTypes[i];
            if (!dataType.isPublished()) {
                if (length != 1) {
                    throw new IllegalStateException(
                            "Received illegal data - probably the connection tried to send a malicious packet!");
                }
                ignore = true;
            }
            final boolean hasDecodeBuffer = framingBuffer != null;
            final ByteBuf decodeBuffer = hasDecodeBuffer ? framingBuffer : buffer;
            final int start = decodeBuffer.readerIndex();
            try {
                data.add(new DataComponent(dataType, dataType.read0(ctx, out, application, decodeBuffer)));
            } catch (CancelReadingSignal signal) {
                // prepare framing of data
                holdingBuffer = Unpooled.buffer(signal.size + buffer.readerIndex() - start);
                buffer.readerIndex(start);
                transferRemaining(decodeBuffer);
                this.packet = packet;
                this.index = index;
                storedData = data;
                hasId = true;
                return;
            }
            if (hasDecodeBuffer) {
                framingBuffer = null;
            }
        }

        tryRelease(buffer);
        if (!ignore) {
            handler.processData(data, ctx.channel());
        }
    }

    // TODO: Check if payloads can be framed correctly!
    private void readPayLoad(@NotNull ByteBuf buffer, @NotNull Channel channel) {
        try {
            ((DataTypeInternalPayload) DataType.getDTIP()).read(application, channel, buffer);
        } catch (CancelReadingSignal signal) {
            // prepare framing of payload
            holdingBuffer = Unpooled.buffer(signal.size);
            transferRemaining(buffer);
            packet = null;
            hasId = true;
        }
    }

    private void tryRelease(@NotNull ByteBuf buffer) {
        if (release && buffer instanceof UnpooledHeapByteBuf) {
            release = false;
            buffer.release();
        }
    }

    private void transferRemaining(@NotNull ByteBuf buffer) {
        final byte[] remaining = ByteBufUtilExtension.readBytes(buffer, buffer.readableBytes());
        tryRelease(buffer);
        if (remaining.length != 0) {
            holdingBuffer.writeBytes(remaining);
        }
        framing = true;
    }

    public void releaseNext() {
        release = true;
    }

    @Override
    public void channelUnregistered(@NotNull ChannelHandlerContext ctx) throws Exception {
        final ConnectionDisconnectEvent event = new ConnectionDisconnectEvent(application.getConnection(ctx.channel()));
        application.getEventManager().callEvent(ListenerType.DISCONNECT, event);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        if (!application.isClient()) {
            ((ConnectionImpl) application.getConnection(ctx.channel())).check();
        }
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable cause) {
        if(cause instanceof OutOfMemoryError) {
            // Don't handle OOM errors because handling them could lead into more OOM errors being thrown.
            return;
        }
        final ExceptionTrowEvent event = new ExceptionTrowEvent(cause);
        application.getEventManager().handleExceptionThrown(event);
    }
}
