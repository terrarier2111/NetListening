package de.terrarier.netlistening.network;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.DataComponent;
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.api.PacketCaching;
import de.terrarier.netlistening.api.encryption.EncryptionSetting;
import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.encryption.hash.HmacUseCase;
import de.terrarier.netlistening.api.event.ExceptionTrowEvent;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.CancelSignal;
import de.terrarier.netlistening.internals.InternalPayloadRegisterPacket;
import de.terrarier.netlistening.internals.InternalUtil;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class PacketDataEncoder extends MessageToByteEncoder<DataContainer> {

    private final ApplicationImpl application;
    private final ExecutorService delayedExecutor;
    private final ConnectionImpl connection;

    public PacketDataEncoder(@NotNull ApplicationImpl application, ExecutorService delayedExecutor,
                             @NotNull ConnectionImpl connection) {
        this.application = application;
        this.delayedExecutor = delayedExecutor;
        this.connection = connection;
    }

    @Override
    protected void encode(@NotNull ChannelHandlerContext ctx, @NotNull DataContainer data, @NotNull ByteBuf buffer) {
        final List<DataComponent<?>> containedData = data.getData();
        final int dataSize = containedData.size();

        if (dataSize < 1) {
            return;
        }

        final DataType<?>[] types = new DataType[dataSize];
        for (int i = 0; i < dataSize; i++) {
            types[i] = containedData.get(i).getType();
        }
        final PacketCache cache = application.getCache();
        PacketSkeleton packet = cache.getPacket(types);

        final int start = buffer.writerIndex();
        try {
            if (packet == null) {
                packet = cache.registerPacket(types);
                final InternalPayloadRegisterPacket register = new InternalPayloadRegisterPacket(packet.getId(), types);
                final ByteBuf registerBuffer = Unpooled.buffer(5 + dataSize);
                DataType.getDTIP().write0(application, registerBuffer, register);
                buffer.writeBytes(ByteBufUtilExtension.getBytes(registerBuffer));
                if (application.getCaching() == PacketCaching.GLOBAL) {
                    cache.broadcastRegister(application, register, ctx.channel(), registerBuffer);
                } else {
                    registerBuffer.release();
                }
                packet.register();
            }

            if (application instanceof Server && !packet.isRegistered()) {
                if (delayedExecutor.isShutdown()) {
                    return;
                }
                final PacketSkeleton finalPacket = packet;
                // Sending data delayed, awaiting the packet's registration to finish
                delayedExecutor.execute(() -> {
                    final Channel channel = ctx.channel();
                    while (!finalPacket.isRegistered());
                    channel.writeAndFlush(data);
                });
                return;
            }

            final EncryptionSetting encryptionSetting = application.getEncryptionSetting();
            final HmacSetting hmacSetting;
            final boolean encrypted = data.isEncrypted();
            if (encryptionSetting == null ||
                    ((hmacSetting = encryptionSetting.getHmacSetting()) == null && !encrypted)) {
                writeToBuffer(buffer, data, packet.getId());
                return;
            }
            final boolean hmac = encrypted || hmacSetting.getUseCase() == HmacUseCase.ALL; // TODO: Include this one in the check above.
            final ByteBuf dst = hmac ? Unpooled.buffer() : buffer;
            final ByteBuf dataBuffer = encrypted ? Unpooled.buffer() : dst;
            writeToBuffer(dataBuffer, data, packet.getId());
            if (encrypted) {
                InternalUtil.writeInt(application, dst, 0x3);
                final byte[] encryptedData = connection.getEncryptionContext().encrypt(
                        ByteBufUtilExtension.getBytes(dataBuffer));
                dataBuffer.release();
                final int size = encryptedData.length;
                ByteBufUtilExtension.correctSize(dst, 4 + size, application.getBuffer());
                dst.writeInt(size);
                dst.writeBytes(encryptedData);
            }

            if (hmac) {
                appendHmac(dst, buffer, connection);
            }
        } catch (CancelSignal ignored) {
			// This is here in order to prevent packets from being sent which contain unserializable data.
            buffer.writerIndex(start);
        }
    }

    private void writeToBuffer(@NotNull ByteBuf buffer, @NotNull DataContainer data, int packetId) throws CancelSignal {
        InternalUtil.writeInt(application, buffer, packetId);
        final List<DataComponent<?>> dataComponentList = data.getData();
        final int dataSize = dataComponentList.size();
        for (int i = 0; i < dataSize; i++) {
            final DataComponent<?> component = dataComponentList.get(i);
            component.getType().writeUnchecked(application, buffer, component.getData());
        }
    }

    private void appendHmac(@NotNull ByteBuf src, @NotNull ByteBuf dst, @NotNull ConnectionImpl connection) {
        final byte[] data = ByteBufUtilExtension.getBytes(src);
        src.release();
        try {
            final byte[] hash = HashUtil.calculateHMAC(data, connection.getHmacKey(),
                    application.getEncryptionSetting().getHmacSetting().getHashingAlgorithm());
            final int buffer = application.getBuffer();
            final int dataLength = data.length;
            final short hashLength = (short) hash.length;
            InternalUtil.writeInt(application, dst, 0x4);
            ByteBufUtilExtension.correctSize(dst, 6 + dataLength + hashLength, buffer);
            dst.writeInt(dataLength);
            dst.writeShort(hashLength);
            dst.writeBytes(data);
            dst.writeBytes(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            application.getEventManager().handleExceptionThrown(new ExceptionTrowEvent(e));
        }
    }

}
