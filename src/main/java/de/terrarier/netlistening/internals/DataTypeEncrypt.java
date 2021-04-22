package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class DataTypeEncrypt extends DataType<Void> {

    public DataTypeEncrypt() {
        super((byte) 0xD, (byte) 4, false);
    }

    @Override
    public Void read0(@AssumeNotNull PacketDataDecoder.DecoderContext decoderContext, @AssumeNotNull List<Object> out,
                      @AssumeNotNull ByteBuf buffer) throws Exception {
        checkReadable(buffer, 4);
        final int size = buffer.readInt();
        checkReadable(buffer, size);
        final byte[] decrypted = decoderContext.getConnection().getEncryptionContext().decrypt(ByteBufUtilExtension.readBytes(buffer, size));
        final PacketDataDecoder decoder = decoderContext.getDecoder();
        final ByteBuf dataBuffer = Unpooled.wrappedBuffer(decrypted);
        decoder.releaseNext();
        decoder.decode(decoderContext.getHandlerContext(), dataBuffer, out);
        return null;
    }

    @Override
    protected Void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                        @AssumeNotNull ByteBuf buffer) {
        return null;
    }

    @Override
    protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, Void empty) {}

}
