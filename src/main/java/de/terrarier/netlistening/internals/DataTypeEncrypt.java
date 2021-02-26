package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class DataTypeEncrypt extends DataType<Void> {

    public DataTypeEncrypt() {
        super((byte) 0xD, (byte) 4, false);
    }

    @Override
    public Void read0(@NotNull ChannelHandlerContext ctx, @NotNull List<Object> out, @NotNull Application application,
            @NotNull ByteBuf buffer) throws Exception {
        checkReadable(buffer, 4);
        final int size = buffer.readInt();
        checkReadable(buffer, size);
        final ConnectionImpl connection = (ConnectionImpl) application.getConnection(null);
        final byte[] decrypted = connection.getEncryptionContext().decrypt(ByteBufUtilExtension.readBytes(buffer, size));
        final PacketDataDecoder decoder = (PacketDataDecoder) ctx.channel().pipeline().get("decoder");
        final ByteBuf dataBuffer = Unpooled.wrappedBuffer(decrypted);
        decoder.releaseNext();
        decoder.decode(ctx, dataBuffer, out);
        return null;
    }

    @Override
    protected Void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
        return null;
    }

    @Override
    protected void write(@NotNull Application application, @NotNull ByteBuf buffer, Void empty) {}

}
