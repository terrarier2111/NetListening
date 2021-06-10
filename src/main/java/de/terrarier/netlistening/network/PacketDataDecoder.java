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
    private final int maxFrameSize;
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
                             @AssumeNotNull ConnectionImpl connection, int maxFrameSize) {
        this.application = application;
        this.handler = handler;
        this.connection = connection;
        this.maxFrameSize = maxFrameSize;
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

        if (framing || readKeepAliveId) {
            if (readKeepAliveId) {
                readKeepAliveId = false;
                readKeepAlive(buffer);
                if (buffer.isReadable()) {
                    decode(ctx, buffer, out); // TODO: Check if this is necessary!
                }
                return;
            }
            // Framing
            final ByteBuf tmp = holdingBuffer;
            final int writable = tmp.writableBytes();
            final boolean block = writable > readable;
            final byte[] remaining = ByteBufUtilExtension.readBytes(buffer, block ? readable : writable);
            tmp.writeBytes(remaining);
            if (block) {
                return;
            }
            framing = false;
            boolean release = false;
            if (hasId) {
                hasId = false;
                if (packet != null) {
                    read(out, storedData, buffer, packet, index, tmp);
                } else {
                    readPayload(tmp);
                }
            } else {
                release = readPacket(buffer, out, tmp);
            }

            if (release || !framing || !tmp.equals(holdingBuffer)) {
                tmp.release();
                if (!framing) {
                    holdingBuffer = null;
                }
            }
            return;
        }

        readPacket(buffer, out, buffer);
    }

    private boolean readPacket(@AssumeNotNull ByteBuf buffer, @AssumeNotNull List<Object> out,
                               @AssumeNotNull ByteBuf idBuffer) throws Exception {
        int id;
        try {
            id = InternalUtil.readInt(application, idBuffer);
            if (application instanceof Server) {
                id = connection.getPacketIdTranslationCache().tryTranslate(id);
            }
        } catch (VarIntUtil.VarIntParseException varIntParseException) {
            // Preparing framing of packet id.
            holdingBuffer = Unpooled.buffer(varIntParseException.requiredBytes);
            transferRemaining(buffer);
            packet = null;
            hasId = false;
            return false;
        }

        switch (id) {
            case 0x1:
                // Handling keep alive packets.
                if (buffer.isReadable()) {
                    readKeepAlive(buffer);
                } else {
                    readKeepAliveId = true;
                }
                break;
            case 0x0:
                readPayload(buffer);
                break;
            default:
                final PacketSkeleton packet = connection.getCache().getPacket(id);
                if (packet == null) {
                    final byte[] data = ConversionUtil.intToBytes(id);

                    if (callInvalidDataEvent(InvalidDataEvent.DataInvalidReason.INVALID_ID, data)) {
                        break;
                    }

                    throw new IllegalStateException("An error occurred while decoding - the packet to decode wasn't recognizable because it wasn't registered before! ("
                            + Integer.toHexString(id) + ')');
                }

                read(out, new ArrayList<>(packet.getData().length), buffer, packet, 0, null);
                break;
        }

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
            final boolean useFramingBuffer = framingBuffer != null;
            final ByteBuf decodeBuffer;
            if (useFramingBuffer) {
                decodeBuffer = framingBuffer;
                framingBuffer = null;
            } else {
                decodeBuffer = buffer;
            }
            final int start = decodeBuffer.readerIndex();
            try {
                data.add(new DataComponent(dataType, dataType.read0(context, out, decodeBuffer)));
            } catch (CancelReadSignal signal) {
                // prepare framing of data
                final int frameSize = signal.size + buffer.readerIndex() - start +
                        (useFramingBuffer ? buffer.readableBytes() : 0);
                handleFraming(frameSize, start, buffer, decodeBuffer);
                if (useFramingBuffer) {
                    transferRemaining(buffer);
                }
                this.packet = packet;
                this.index = i;
                storedData = data;
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

    private void readPayload(@AssumeNotNull ByteBuf buffer) {
        final int start = buffer.readerIndex();
        if (!buffer.isReadable()) {
            handleFraming(1, start, buffer, buffer);
            packet = null;
            return;
        }
        try {
            DataType.getDTIP().read(application, connection, buffer);
        } catch (CancelReadSignal signal) {
            // prepare framing of payload
            final int frameSize = signal.size + buffer.readerIndex() - start;
            handleFraming(frameSize, start, buffer, buffer);
            packet = null;
        }
    }

    private void handleFraming(int frameSize, int start, @AssumeNotNull ByteBuf buffer,
                               @AssumeNotNull ByteBuf decodeBuffer) {
        if (frameSize > maxFrameSize) {
            final byte[] data = new byte[8];
            // Note: The first 4 bytes represent 0x0 in it's int representation.
            ConversionUtil.intToBytes(data, 4, frameSize);
            if (!callInvalidDataEvent(InvalidDataEvent.DataInvalidReason.TOO_LARGE_FRAME, data)) {
                tryRelease(buffer);
                throw new IllegalStateException("Received a frame which is too large. (size: " + frameSize +
                        " | max: " + maxFrameSize + ')');
            }
        }
        holdingBuffer = Unpooled.buffer(frameSize);
        decodeBuffer.readerIndex(start);
        transferRemaining(decodeBuffer);
        hasId = true;
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
        if (keepAliveId != nextId && application instanceof Server/* && false*/) { // Disable buggy check temporarily until it's fixed.
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
        application.getEventManager().handleExceptionThrown(cause);
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
