package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.api.encryption.hash.HashUtil;
import de.terrarier.netlistening.api.event.LengthExtensionDetectionEvent;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.network.PacketDataDecoder;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;

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
    public Void read0(@AssumeNotNull PacketDataDecoder.DecoderContext context, @AssumeNotNull List<Object> out,
                      @AssumeNotNull ByteBuf buffer) throws Exception {
        checkReadable(buffer, 6);
        final int size = buffer.readInt();
        final short hashSize = buffer.readShort();
        checkReadable(buffer, size + hashSize);
        final byte[] traffic = ByteBufUtilExtension.readBytes(buffer, size);
        final byte[] hash = ByteBufUtilExtension.readBytes(buffer, hashSize);
        final byte[] computedHash = HashUtil.calculateHMAC(traffic, context.getConnection().getHmacKey(),
                context.getApplication().getEncryptionSetting().getHmacSetting().getHashingAlgorithm());
        if(!Arrays.equals(hash, computedHash)) {
            final LengthExtensionDetectionEvent event = new LengthExtensionDetectionEvent(hash, computedHash);
            if(event.getResult() == LengthExtensionDetectionEvent.Result.DROP_DATA) {
                return null;
            }
        }
        final PacketDataDecoder decoder = context.getDecoder();
        final ByteBuf dataBuffer = Unpooled.wrappedBuffer(traffic);
        decoder.releaseNext();
        decoder.decode(context.getHandlerContext(), dataBuffer, out);
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
