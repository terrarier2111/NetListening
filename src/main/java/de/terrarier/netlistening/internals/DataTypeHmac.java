package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.event.LengthExtensionDetectionEvent;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class DataTypeHmac extends DataType<Void> {

    public DataTypeHmac() {
        super((byte) 0xE, (byte) 6, false);
    }

    @Override
    public Void read0(@NotNull ChannelHandlerContext ctx, @NotNull List<Object> out, @NotNull ApplicationImpl application,
                      @NotNull ConnectionImpl connection, @NotNull ByteBuf buffer) throws Exception {
        checkReadable(buffer, 6);
        final int size = buffer.readInt();
        final short hashSize = buffer.readShort();
        checkReadable(buffer, size + hashSize);
        final byte[] traffic = ByteBufUtilExtension.readBytes(buffer, size);
        final byte[] hash = ByteBufUtilExtension.readBytes(buffer, hashSize);
        final byte[] computedHash = HashUtil.calculateHMAC(traffic, connection.getHmacKey(),
                application.getEncryptionSetting().getHmacSetting().getHashingAlgorithm());
        if(!Arrays.equals(hash, computedHash)) {
            final LengthExtensionDetectionEvent event = new LengthExtensionDetectionEvent(hash, computedHash);
            if(event.getResult() == LengthExtensionDetectionEvent.Result.DROP_DATA) {
                return null;
            }
        }
        final PacketDataDecoder decoder = (PacketDataDecoder) ctx.channel().pipeline().get(Application.DECODER);
        final ByteBuf dataBuffer = Unpooled.wrappedBuffer(traffic);
        decoder.releaseNext();
        decoder.decode(ctx, dataBuffer, out);
        return null;
    }

    @Override
    protected Void read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection,
                        @NotNull ByteBuf buffer) {
        return null;
    }

    @Override
    protected void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, Void empty) {}

}
