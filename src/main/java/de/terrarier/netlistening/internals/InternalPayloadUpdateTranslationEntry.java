package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @author Terrarier2111
 * @since 1.11
 */
@ApiStatus.Internal
public final class InternalPayloadUpdateTranslationEntry extends InternalPayload {

    private final int id;
    private int newId = -1;

    InternalPayloadUpdateTranslationEntry(int id) {
        super((byte) 0x6);
        this.id = id;
    }

    public InternalPayloadUpdateTranslationEntry(int id, int newId) {
        this(id);
        this.newId = newId;
    }

    @Override
    void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer) {
        InternalUtil.writeInt(application, buffer, id);
        if (newId != -1) {
            InternalUtil.writeInt(application, buffer, newId);
        }
    }

    @Override
    void read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection, @NotNull ByteBuf buffer)
            throws CancelReadSignal {
        final int id;
        try {
            id = InternalUtil.readInt(application, buffer);
        } catch (VarIntUtil.VarIntParseException e) {
            throw new CancelReadSignal(e.requiredBytes);
        }
        if (application instanceof Server) {
            connection.getPacketIdTranslationCache().delete(id);
        } else {
            final int newId;
            try {
                newId = InternalUtil.readInt(application, buffer);
            } catch (VarIntUtil.VarIntParseException e) {
                throw new CancelReadSignal(e.requiredBytes);
            }
            application.getCache().swapId(id, newId);
            final ByteBuf translationUpdateBuffer = Unpooled.buffer(InternalUtil.getSingleByteSize(application) + 1 + 4); // TODO: Improve init size.
            DataType.getDTIP().write0(application, translationUpdateBuffer,
                    new InternalPayloadUpdateTranslationEntry(id));
            final Channel channel = connection.getChannel();
            channel.writeAndFlush(translationUpdateBuffer, channel.voidPromise());
        }
    }

}
