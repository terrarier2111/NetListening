package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.event.*;
import de.terrarier.netlistening.api.type.DataType;
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

    private final Application application;
    private final DataHandler handler;
    private final EventManager eventManager;
    private boolean framing;
    private ByteBuf holdingBuffer;
    private ArrayList<DataComponent<?>> storedData;
    private int index;
    private PacketSkeleton packet;
    private boolean hasId;
    private boolean release;

    public PacketDataDecoder(@NotNull Application application, @NotNull DataHandler handler, @NotNull EventManager eventManager) {
        this.application = application;
        this.handler = handler;
        this.eventManager = eventManager;
    }

    @Override
    public void decode(@NotNull ChannelHandlerContext ctx, @NotNull ByteBuf buffer, @NotNull List<Object> dataComp) throws Exception {
        final int readable = buffer.readableBytes();
        if (framing) {
            // Framing
            final ByteBuf tmp = holdingBuffer;
            final int writable = tmp.writableBytes();
            if (writable > readable) {
                final byte[] remaining = ByteBufUtilExtension.readBytes(buffer, readable);
                if (remaining.length != 0) { // check for an empty packet, should never occur
                    tmp.writeBytes(remaining);
                }
                return;
            }
            final byte[] remaining = ByteBufUtilExtension.readBytes(buffer, writable);
            if (remaining.length != 0) { // check for an empty packet, should never occur
                tmp.writeBytes(remaining);
            }
            framing = false;
            boolean release = false;
            if (!hasId) {
                final boolean[] idReadValidator = new boolean[1];
                readPacket(ctx, buffer, dataComp, tmp, idReadValidator);
                release = idReadValidator[0];
            } else {
                hasId = false;
                if (packet != null) {
                    read(ctx, dataComp, storedData, buffer, packet, index, tmp);
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
            eventManager.callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE, (EventManager.EventProvider<InvalidDataEvent>) () -> {
                final Connection connection = application.getConnection(ctx.channel());
                return new InvalidDataEvent(connection, DataInvalidReason.EMPTY_PACKET, EmptyArrays.EMPTY_BYTES);
            });
            throw new IllegalStateException("Received an empty packet!");
        }
        readPacket(ctx, buffer, dataComp);
    }

    private void readPacket(@NotNull ChannelHandlerContext ctx, @NotNull ByteBuf buffer, @NotNull List<Object> dataComp) throws Exception {
        readPacket(ctx, buffer, dataComp, null, null);
    }

    private void readPacket(@NotNull ChannelHandlerContext ctx, @NotNull ByteBuf buffer, @NotNull List<Object> dataComp,
                            ByteBuf idBuffer, boolean[] packetIdReadValidator) throws Exception {
        int id;
        try {
            id = InternalUtil.readInt(application, idBuffer != null ? idBuffer : buffer);
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
            buffer.readByte();
            return;
        }

        if (id == 0x2) {
            if (!application.isClient()) {
                eventManager.callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE, (EventManager.EventProvider<InvalidDataEvent>) () -> {
                    final Connection connection = application.getConnection(ctx.channel());
                    final byte[] data = application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.toVarInt(0x2) : ConversionUtil.intToByteArray(0x2);
                    return new InvalidDataEvent(connection, DataInvalidReason.MALICIOUS_ACTION, data);
                });
                throw new IllegalStateException("Received malicious data! (0x2)");
            }

            ((ClientImpl) application).pushCachedData();
            if (buffer.isReadable()) {
                decode(ctx, buffer, dataComp);
            }
            return;
        }

        if (!buffer.isReadable()) {
            eventManager.callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE, (EventManager.EventProvider<InvalidDataEvent>) () -> {
                final Connection connection = application.getConnection(ctx.channel());
                final byte[] data = application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.toVarInt(id) : ConversionUtil.intToByteArray(id);
                return new InvalidDataEvent(connection, DataInvalidReason.INCOMPLETE_PACKET, data);
            });

            throw new IllegalStateException(
                    "An error occurred while decoding - the packet to decode was empty! (skipping current packet with id: " + Integer.toHexString(id) + ")");
        }

        if (id == 0x0) {
            readPayLoad(buffer, ctx.channel());
            return;
        }

        final ConnectionImpl connection = (ConnectionImpl) application.getConnection(ctx.channel());
        final PacketSkeleton packet = connection.getCache().getInPacketFromId(id);
        if (packet == null) {

            eventManager.callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE, (EventManager.EventProvider<InvalidDataEvent>) () -> {
                final byte[] data = application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.toVarInt(id) : ConversionUtil.intToByteArray(id);
                return new InvalidDataEvent(connection, DataInvalidReason.INVALID_ID, data);
            });

            throw new IllegalStateException(
                    "An error occurred while decoding - the packet to decode wasn't recognizable because it wasn't registered before! (" + Integer.toHexString(id) + ")");
        }

        read(ctx, dataComp, new ArrayList<>(), buffer, packet, 0, null);
    }

    private void read(@NotNull ChannelHandlerContext ctx, @NotNull List<Object> comp, @NotNull ArrayList<DataComponent<?>> dataCollection,
                      @NotNull ByteBuf buffer, @NotNull PacketSkeleton packet, int index, ByteBuf framingBuffer) throws Exception {
        final DataType<?>[] dataArray = packet.getData();
        final int length = dataArray.length;
        boolean ignore = false;

        for (int i = index; i < length; i++) {
            final DataType<?> data = dataArray[i];
            if (!data.isPublished()) {
                if (length != 1) {
                    throw new IllegalStateException("Received illegal data - probably the connection tried to send a malicious packet!");
                }
                ignore = true;
            }
            final boolean useOptionalBuffer = framingBuffer != null;
            try {
                readSingle(ctx, comp, dataCollection, useOptionalBuffer ? framingBuffer : buffer, packet, i);
            } catch (CancelReadingSignal signal) {
                // This is here in order to interrupt the method execution if framing is required
                return;
            }
            if (useOptionalBuffer) {
                framingBuffer = null;
            }
        }

        tryRelease(buffer);
        if (!ignore) {
            handler.processData(dataCollection, ctx.channel());
        }
    }

    @SuppressWarnings("unchecked")
    private void readSingle(@NotNull ChannelHandlerContext ctx, @NotNull List<Object> comp, @NotNull ArrayList<DataComponent<?>> dataCollection,
                            @NotNull ByteBuf buffer, @NotNull PacketSkeleton packet, int index) throws Exception {
        final DataType<?> data = packet.getData()[index];
        try {
            dataCollection.add(new DataComponent(data, data.read0(ctx, comp, application, buffer)));
        } catch (CancelReadingSignal signal) {
            // prepare framing of data
            final int signalSize = signal.size;
            final boolean array = signal.array;
            holdingBuffer = Unpooled.buffer((array ? 4 : 0) + signalSize);
            if (array) {
                holdingBuffer.writeInt(signalSize);
            }
            transferRemaining(buffer);
            this.packet = packet;
            this.index = index;
            storedData = dataCollection;
            hasId = true;
            throw signal;
        }
    }

    // TODO: Check if payloads can be framed correctly!
    private void readPayLoad(@NotNull ByteBuf buffer, @NotNull Channel channel) {
        try {
            ((DataTypeInternalPayload) DataType.getDTIP()).read(application, channel, buffer);
        } catch (CancelReadingSignal signal) {
            // prepare framing of payload
            holdingBuffer = Unpooled.buffer(signal.size + 1);
            transferRemaining(buffer);
            packet = null;
            hasId = true;
        }
    }

    private void tryRelease(@NotNull ByteBuf buffer) {
        if (release && buffer instanceof UnpooledHeapByteBuf) {
            buffer.release();
            release = false;
        }
    }

    private void transferRemaining(@NotNull ByteBuf buffer) {
        final byte[] remaining = ByteBufUtilExtension.readBytes(buffer);
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
        eventManager.callEvent(ListenerType.DISCONNECT, event);
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
        final ExceptionTrowEvent event = new ExceptionTrowEvent(cause);
        eventManager.callEvent(ListenerType.EXCEPTION_THROW, event);
        if (event.isPrint()) {
            event.getException().printStackTrace();
        }
    }
}
