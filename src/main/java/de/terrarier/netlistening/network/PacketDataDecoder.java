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
package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.event.*;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
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
import io.netty.util.internal.SystemPropertyUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class PacketDataDecoder extends ByteToMessageDecoder {

    private static final boolean IGNORE_EMPTY_PACKETS = SystemPropertyUtil.getBoolean(
            "de.terrarier.netlistening.IgnoreEmptyPackets", true);

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
    private byte lastKeepAliveId = Byte.MIN_VALUE;
    private boolean readKeepAliveId;

    public PacketDataDecoder(@AssumeNotNull ApplicationImpl application, @AssumeNotNull DataHandler handler,
                             @AssumeNotNull ConnectionImpl connection) {
        this.application = application;
        this.handler = handler;
        this.connection = connection;
    }

    @Override
    public void decode(@AssumeNotNull ChannelHandlerContext ctx, @AssumeNotNull ByteBuf buffer,
                       @AssumeNotNull List<Object> out) throws Exception {
        final int readable = buffer.readableBytes();
        // This prevents empty packets from being decoded after the connection was closed.
        if (readable < 1) {
            if (IGNORE_EMPTY_PACKETS || !ctx.channel().isActive() ||
                    callInvalidDataEvent(InvalidDataEvent.DataInvalidReason.EMPTY_PACKET, EmptyArrays.EMPTY_BYTES)) {
                return;
            }
            throw new IllegalStateException("Received an empty packet!");
        }

        if (framing) {
            // Framing
            final ByteBuf tmp = holdingBuffer;
            final int writable = tmp.writableBytes();
            final boolean block = writable > readable;
            final byte[] remaining = ByteBufUtilExtension.readBytes(buffer, block ? readable : writable);
            if (IGNORE_EMPTY_PACKETS || remaining.length != 0) { // check for empty packets, should never occur
                tmp.writeBytes(remaining);
            }
            if (block) {
                return;
            }
            framing = false;
            boolean release = false;
            if (!hasId) {
                release = readPacket(buffer, out, tmp);
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

        if (readKeepAliveId) {
            readKeepAliveId = false;
            readKeepAlive(buffer);
            if (!buffer.isReadable()) {
                return;
            }
        }

        readPacket(buffer, out, buffer);
    }

    private boolean readPacket(@AssumeNotNull ByteBuf buffer, @AssumeNotNull List<Object> out,
                               @AssumeNotNull ByteBuf idBuffer) throws Exception {
        final int id;
        try {
            id = InternalUtil.readInt(application, idBuffer);
        } catch (VarIntUtil.VarIntParseException varIntParseException) {
            // preparing framing of packet id
            holdingBuffer = Unpooled.buffer(varIntParseException.requiredBytes);
            transferRemaining(buffer);
            packet = null;
            hasId = false;
            return false;
        }

        if (id == 0x1) {
            // Handling keep alive packets.
            if (buffer.isReadable()) {
                readKeepAlive(buffer);
            } else {
                readKeepAliveId = true;
            }
            return true;
        }

        if (id == 0x2) {
            if (application instanceof Server) {
                // TODO: We should probably cache this byte array.
                final byte[] data = ConversionUtil.intToBytes(0x2);

                if (callInvalidDataEvent(InvalidDataEvent.DataInvalidReason.MALICIOUS_ACTION, data)) {
                    return true;
                }

                throw new IllegalStateException("Received malicious data! (0x2)");
            }

            ((ClientImpl) application).pushCachedData();
            if (buffer.isReadable()) {
                decode(context.getHandlerContext(), buffer, out);
            }
            return true;
        }

        if (!buffer.isReadable()) {
            final byte[] data = ConversionUtil.intToBytes(id);

            if (callInvalidDataEvent(InvalidDataEvent.DataInvalidReason.INCOMPLETE_PACKET, data)) {
                return true;
            }

            throw new IllegalStateException("An error occurred while decoding - the packet to decode was empty! (skipping current packet with id: "
                    + Integer.toHexString(id) + ')');
        }

        if (id == 0x0) {
            readPayLoad(buffer);
            return true;
        }

        final PacketSkeleton packet = connection.getCache().getPacket(id);
        if (packet == null) {
            final byte[] data = ConversionUtil.intToBytes(id);

            if (callInvalidDataEvent(InvalidDataEvent.DataInvalidReason.INVALID_ID, data)) {
                return true;
            }

            throw new IllegalStateException("An error occurred while decoding - the packet to decode wasn't recognizable because it wasn't registered before! ("
                    + Integer.toHexString(id) + ')');
        }

        read(out, new ArrayList<>(packet.getData().length), buffer, packet, 0, null);
        return true;
    }

    @SuppressWarnings("unchecked")
    private void read(@AssumeNotNull List<Object> out, @AssumeNotNull List<DataComponent<?>> data,
                      @AssumeNotNull ByteBuf buffer, @AssumeNotNull PacketSkeleton packet, int index,
                      ByteBuf framingBuffer) throws Exception {
        final DataType<?>[] dataTypes = packet.getData();
        final int length = dataTypes.length;
        boolean ignore = false;

        for (int i = index; i < length; i++) {
            final DataType<?> dataType = dataTypes[i];
            if (!dataType.isPublished()) {
                ignore = true;
            }
            final boolean useFramingBuffer;
            final ByteBuf decodeBuffer;
            if (framingBuffer != null) {
                useFramingBuffer = true;
                decodeBuffer = framingBuffer;
                framingBuffer = null;
            } else {
                useFramingBuffer = false;
                decodeBuffer = buffer;
            }
            final int start = decodeBuffer.readerIndex();
            try {
                data.add(new DataComponent(dataType, dataType.read0(context, out, decodeBuffer)));
            } catch (CancelReadSignal signal) {
                // prepare framing of data
                holdingBuffer = Unpooled.buffer(signal.size + decodeBuffer.readerIndex() - start +
                        (useFramingBuffer ? buffer.readableBytes() : 0));
                decodeBuffer.readerIndex(start);
                transferRemaining(decodeBuffer);
                if (useFramingBuffer) {
                    transferRemaining(buffer);
                }
                this.packet = packet;
                this.index = i;
                storedData = data;
                hasId = true;
                return;
            } catch (CancelSignal signal) {
                // Handling cases in which objects can't get deserialized.
                if (i + 1 == length) {
                    tryRelease(buffer);
                    return;
                }
                invalidData = true;
                if (!buffer.isReadable()) {
                    // prepare framing for data which can be discarded
                    this.packet = packet;
                    this.index = i;
                    storedData = data;
                    hasId = true;
                    holdingBuffer = Unpooled.buffer(dataTypes[i + 1].getMinSize());
                    framing = true;
                    tryRelease(buffer);
                    return;
                }
            }

        }

        tryRelease(buffer);
        if (invalidData) {
            invalidData = false;
            return;
        }
        if (!ignore) {
            // Passing the result to the decode listeners (if present).
            handler.processData(data, connection);
        }
    }

    private void readPayLoad(@AssumeNotNull ByteBuf buffer) {
        final int start = buffer.readerIndex();
        try {
            DataType.getDTIP().read(application, connection, buffer);
        } catch (CancelReadSignal signal) {
            // prepare framing of payload
            final int frameSize = signal.size + buffer.readerIndex() - start;
            final ConnectionDataFrameEvent event = new ConnectionDataFrameEvent(connection, signal.size,
                    frameSize - signal.size);
            if (!application.getEventManager().callEvent(ListenerType.FRAME, EventManager.CancelAction.INTERRUPT,
                    event)) {
                holdingBuffer = Unpooled.buffer(frameSize);
                buffer.readerIndex(start);
                transferRemaining(buffer);
                packet = null;
                hasId = true;
            } else {
                tryRelease(buffer);
            }
        }
    }

    private void tryRelease(@AssumeNotNull ByteBuf buffer) {
        if (release && buffer instanceof UnpooledHeapByteBuf) {
            release = false;
            buffer.release();
        }
    }

    private boolean callInvalidDataEvent(@AssumeNotNull InvalidDataEvent.DataInvalidReason reason,
                                         @AssumeNotNull byte[] data) {
        final InvalidDataEvent event = new InvalidDataEvent(connection, reason, data);
        return application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                event);
    }

    private void readKeepAlive(@AssumeNotNull ByteBuf buffer) {
        final byte keepAliveId = buffer.readByte();
        final byte lastKeepAliveId = this.lastKeepAliveId;
        final byte nextId = (byte) ((lastKeepAliveId == Byte.MAX_VALUE ? Byte.MIN_VALUE : lastKeepAliveId) + 1);
        this.lastKeepAliveId = nextId;
        if (keepAliveId != nextId && application instanceof Server && false) { // Disable buggy check temporarily until it's fixed.
            final byte[] data = new byte[]{lastKeepAliveId, nextId, keepAliveId};

            if (callInvalidDataEvent(InvalidDataEvent.DataInvalidReason.INVALID_KEEP_ALIVE_ID, data)) {
                return;
            }

            throw new IllegalStateException("Received a keep alive packet with an invalid id! (expected: " + nextId +
                    " received: " + keepAliveId + ')');
        }
    }

    private void transferRemaining(@AssumeNotNull ByteBuf buffer) {
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
    public void channelUnregistered(@AssumeNotNull ChannelHandlerContext ctx) throws Exception {
        final ConnectionDisconnectEvent event = new ConnectionDisconnectEvent(connection);
        application.getEventManager().callEvent(ListenerType.DISCONNECT, event);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(@AssumeNotNull ChannelHandlerContext ctx) throws Exception {
        context = new DecoderContext(application, connection, this, ctx);
        if (application instanceof Server) {
            // Check if we have to initialize stuff in the connection.
            connection.check();
        }
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(@AssumeNotNull ChannelHandlerContext ctx, @AssumeNotNull Throwable cause) {
        if (cause instanceof OutOfMemoryError) {
            // Don't handle OOM errors because handling them could lead into more OOM errors being thrown.
            return;
        }
        if (cause instanceof ThreadDeath) {
            // Don't handle ThreadDeath
            return;
        }
        final ExceptionTrowEvent event = new ExceptionTrowEvent(cause);
        application.getEventManager().handleExceptionThrown(event);
    }

    @ApiStatus.Internal
    public static final class DecoderContext {

        private final ApplicationImpl application;
        private final ConnectionImpl connection;
        private final PacketDataDecoder decoder;
        private final ChannelHandlerContext handlerContext;

        private DecoderContext(@AssumeNotNull ApplicationImpl application,
                               @AssumeNotNull ConnectionImpl connection,
                               @AssumeNotNull PacketDataDecoder decoder,
                               @AssumeNotNull ChannelHandlerContext handlerContext) {
            this.application = application;
            this.connection = connection;
            this.decoder = decoder;
            this.handlerContext = handlerContext;
        }

        @AssumeNotNull
        public ApplicationImpl getApplication() {
            return application;
        }

        @AssumeNotNull
        public ConnectionImpl getConnection() {
            return connection;
        }

        @AssumeNotNull
        public PacketDataDecoder getDecoder() {
            return decoder;
        }

        @AssumeNotNull
        public ChannelHandlerContext getHandlerContext() {
            return handlerContext;
        }

    }

}
