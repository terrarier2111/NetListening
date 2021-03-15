package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public abstract class InternalPayload {

    private static final InternalPayload_RegisterPacket REGISTER_PACKET = new InternalPayload_RegisterPacket(-1);
    public static final InternalPayload_Handshake HANDSHAKE = new InternalPayload_Handshake();
    static final InternalPayload_EncryptionInit ENCRYPTION_INIT = new InternalPayload_EncryptionInit();
    static final InternalPayload_EncryptionFinish ENCRYPTION_FINISH = new InternalPayload_EncryptionFinish();

    private final byte id;

    InternalPayload(byte id) {
        this.id = id;
    }

    final void write0(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer) {
        checkWriteable(application, buffer, 1);
        buffer.writeByte(id);
        write(application, buffer);
    }

    abstract void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer);

    public abstract void read(@NotNull ApplicationImpl application, @NotNull Channel channel, @NotNull ByteBuf buffer)
            throws CancelReadingSignal;

    @NotNull
    static InternalPayload fromId(byte id) {
        switch (id) {
            case 0x1:
                return REGISTER_PACKET;
            case 0x2:
                return HANDSHAKE;
            case 0x3:
                return ENCRYPTION_INIT;
            case 0x4:
                return ENCRYPTION_FINISH;
            default:
                throw new IllegalArgumentException("Tried to process an internal payload with an invalid id! (" + Integer.toHexString(id) + ")");
        }
    }

    static void checkReadable(@NotNull ByteBuf buffer, int additional)
            throws CancelReadingSignal {
        if (buffer.readableBytes() < additional) {
            throw new CancelReadingSignal(additional);
        }
    }

    static void checkWriteable(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, int length) {
        ByteBufUtilExtension.correctSize(buffer, length, application.getBuffer());
    }

}
