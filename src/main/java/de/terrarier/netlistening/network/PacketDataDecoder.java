package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.event.*;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.CancelReadSignal;
import de.terrarier.netlistening.internals.CancelSignal;
import de.terrarier.netlistening.internals.InternalUtil;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import de.terrarier.netlistening.utils.ConversionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledHeapByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.EmptyArrays;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class PacketDataDecoder extends ByteToMessageDecoder {

    private final ApplicationImpl application;
    private final DataHandler handler;
    private final ConnectionImpl connection;
    private DecoderContext context;
    private boolean framing;
    private ByteBuf holdingBuffer;
    private List<DataComponent<?>> storedData;
    private int index;
    private PacketSkeleton packet;
    private boolean hasId;
    private boolean invalidData;
    private boolean release;

    public PacketDataDecoder(@NotNull ApplicationImpl application, @NotNull DataHandler handler,
                             @NotNull ConnectionImpl connection) {
        this.application = application;
        this.handler = handler;
        this.connection = connection;
    }

    @Override
    public void decode(@NotNull ChannelHandlerContext ctx, @NotNull ByteBuf buffer, @NotNull List<Object> out)
            throws Exception {

        // This prevents empty buffers from being decoded after the connection was closed.
        // TODO: Check if we should ignore all and not only empty buffers.
        if (!ctx.channel().isActive() && !buffer.isReadable()) {
            return;
        }

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
                readPacket(buffer, out, tmp, idReadValidator);
                release = idReadValidator[0];
            } else {
                hasId = false;
                if (packet != null) {
                    read(out, storedData, buffer, packet, index, tmp);
                } else {
                    readPayLoad(tmp);
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
            if(application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    (EventManager.EventProvider<InvalidDataEvent>) () -> new InvalidDataEvent(connection, InvalidDataEvent.DataInvalidReason.EMPTY_PACKET,
                            EmptyArrays.EMPTY_BYTES))) return;

            throw new IllegalStateException("Received an empty packet!");
        }
        readPacket(buffer, out, buffer, null);
    }

    private void readPacket(@NotNull ByteBuf buffer, @NotNull List<Object> out,
                            @NotNull ByteBuf idBuffer, boolean[] packetIdReadValidator) throws Exception {
        final int id;
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
            // Dropping the keep alive packet content.
            buffer.skipBytes(1);
            return;
        }

        if (id == 0x2) {
            if (application instanceof Server) {
                if(application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                        (EventManager.EventProvider<InvalidDataEvent>) () -> {
                    final byte[] data = application.getCompressionSetting().isVarIntCompression()
                            ? VarIntUtil.toVarInt(0x2) : ConversionUtil.intToBytes(0x2);

                    return new InvalidDataEvent(connection, InvalidDataEvent.DataInvalidReason.MALICIOUS_ACTION, data);
                })) return;

                throw new IllegalStateException("Received malicious data! (0x2)");
            }

            ((ClientImpl) application).pushCachedData();
            if (buffer.isReadable()) {
                decode(context.getHandlerContext(), buffer, out);
            }
            return;
        }

        if (!buffer.isReadable()) {
            if(application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    (EventManager.EventProvider<InvalidDataEvent>) () -> {
                final byte[] data = application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.toVarInt(id)
                        : ConversionUtil.intToBytes(id);

                return new InvalidDataEvent(connection, InvalidDataEvent.DataInvalidReason.INCOMPLETE_PACKET, data);
            })) return;

            throw new IllegalStateException("An error occurred while decoding - the packet to decode was empty! (skipping current packet with id: "
                            + Integer.toHexString(id) + ")");
        }

        if (id == 0x0) {
            readPayLoad(buffer);
            return;
        }

        final PacketSkeleton packet = connection.getCache().getPacket(id);
        if (packet == null) {
            if(application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    (EventManager.EventProvider<InvalidDataEvent>) () -> {
                final byte[] data = application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.toVarInt(id)
                        : ConversionUtil.intToBytes(id);

                return new InvalidDataEvent(connection, InvalidDataEvent.DataInvalidReason.INVALID_ID, data);
            })) return;

            throw new IllegalStateException("An error occurred while decoding - the packet to decode wasn't recognizable because it wasn't registered before! ("
                            + Integer.toHexString(id) + ")");
        }

        read(out, new ArrayList<>(packet.getData().length), buffer, packet, 0, null);
    }

    @SuppressWarnings("unchecked")
    private void read(@NotNull List<Object> out, @NotNull List<DataComponent<?>> data,
                      @NotNull ByteBuf buffer, @NotNull PacketSkeleton packet, int index, ByteBuf framingBuffer)
            throws Exception {
        final DataType<?>[] dataTypes = packet.getData();
        final int length = dataTypes.length;
        boolean ignore = false;

        for (int i = index; i < length; i++) {
            final DataType<?> dataType = dataTypes[i];
            if (!dataType.isPublished()) {
                ignore = true;
            }
            final boolean useFramingBuffer = framingBuffer != null;
            final ByteBuf decodeBuffer = useFramingBuffer ? framingBuffer : buffer;
            final int start = decodeBuffer.readerIndex();
            try {
                data.add(new DataComponent(dataType, dataType.read0(context, out, decodeBuffer)));
            } catch (CancelReadSignal signal) {
                // prepare framing of data
                holdingBuffer = Unpooled.buffer(signal.size + decodeBuffer.readerIndex() - start +
                        (useFramingBuffer ? buffer.readableBytes() : 0));
                decodeBuffer.readerIndex(start);
                transferRemaining(decodeBuffer);
                if(useFramingBuffer) {
                    transferRemaining(buffer);
                }
                this.packet = packet;
                this.index = i;
                storedData = data;
                hasId = true;
                return;
            } catch (CancelSignal signal) {
                if(i + 1 < length) {
                    // TODO: Test cancel logic - this occurs when an invalid object is being deserialized.
                    invalidData = true;
                    final int readable = buffer.readableBytes();
                    if(readable != 0) {
                        decode(context.getHandlerContext(), buffer, out);
                        return;
                    }else {
                        this.packet = packet;
                        this.index = i;
                        storedData = data;
                        hasId = true;
                        holdingBuffer = Unpooled.buffer(dataTypes[i + 1].getMinSize());
                        framing = true;
                    }
                }
                tryRelease(buffer);
                return;
            }
            if (useFramingBuffer) {
                framingBuffer = null;
            }
        }

        tryRelease(buffer);
        if(invalidData) {
            invalidData = false;
            return;
        }
        if (!ignore) {
            handler.processData(data, connection);
        }
    }

    private void readPayLoad(@NotNull ByteBuf buffer) {
        final int start = buffer.readerIndex();
        try {
            DataType.getDTIP().read(application, connection, buffer);
        } catch (CancelReadSignal signal) {
            // prepare framing of payload
            final int frameSize = signal.size + buffer.readerIndex() - start;
            if(!callFrameEvent(frameSize)) {
                holdingBuffer = Unpooled.buffer(frameSize);
                buffer.readerIndex(start);
                transferRemaining(buffer);
                packet = null;
                hasId = true;
            }else {
                tryRelease(buffer);
            }
        }
    }

    private void tryRelease(@NotNull ByteBuf buffer) {
        if (release && buffer instanceof UnpooledHeapByteBuf) {
            release = false;
            buffer.release();
        }
    }

    private boolean callFrameEvent(int frameSize) {
        final ConnectionDataFrameEvent frameEvent = new ConnectionDataFrameEvent(connection, frameSize);
        return application.getEventManager().callEvent(ListenerType.FRAME, EventManager.CancelAction.INTERRUPT,
                frameEvent);
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
        final ConnectionDisconnectEvent event = new ConnectionDisconnectEvent(connection);
        application.getEventManager().callEvent(ListenerType.DISCONNECT, event);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        context = new DecoderContext(application, connection, this, ctx);
        if (application instanceof Server) {
            connection.check();
        }
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable cause) {
        if(cause instanceof OutOfMemoryError) {
            // Don't handle OOM errors because handling them could lead into more OOM errors being thrown.
            return;
        }
        if(cause instanceof ThreadDeath) {
            // Don't handle ThreadDeath
            return;
        }
        final ExceptionTrowEvent event = new ExceptionTrowEvent(cause);
        application.getEventManager().handleExceptionThrown(event);
    }

    public static final class DecoderContext {

        private final ApplicationImpl application;
        private final ConnectionImpl connection;
        private final PacketDataDecoder decoder;
        private final ChannelHandlerContext handlerContext;

        public DecoderContext(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection,
                              @NotNull PacketDataDecoder decoder, @NotNull ChannelHandlerContext handlerContext) {
            this.application = application;
            this.connection = connection;
            this.decoder = decoder;
            this.handlerContext = handlerContext;
        }

        @NotNull
        public ApplicationImpl getApplication() {
            return application;
        }

        @NotNull
        public ConnectionImpl getConnection() {
            return connection;
        }

        @NotNull
        public PacketDataDecoder getDecoder() {
            return decoder;
        }

        @NotNull
        public ChannelHandlerContext getHandlerContext() {
            return handlerContext;
        }

    }

}
