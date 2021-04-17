package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public abstract class InternalPayload {

    private static final InternalPayloadRegisterPacket REGISTER_PACKET = new InternalPayloadRegisterPacket(-1);
    public static final InternalPayloadHandshake HANDSHAKE = new InternalPayloadHandshake();
    static final InternalPayloadEncryptionInit ENCRYPTION_INIT = new InternalPayloadEncryptionInit();
    static final InternalPayloadEncryptionFinish ENCRYPTION_FINISH = new InternalPayloadEncryptionFinish();
    private static final InternalPayloadUpdateTranslationEntry UPDATE_TRANSLATION_ENTRY = new InternalPayloadUpdateTranslationEntry(-1);

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

    public abstract void read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection, @NotNull ByteBuf buffer)
            throws CancelReadSignal;

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
            case 0x5:
                return UPDATE_TRANSLATION_ENTRY;
            default:
                throw new IllegalArgumentException("Tried to process an internal payload with an invalid id! (" +
                        Integer.toHexString(id) + ")");
        }
    }

    static void checkReadable(@NotNull ByteBuf buffer, int additional)
            throws CancelReadSignal {
        if (buffer.readableBytes() < additional) {
            throw new CancelReadSignal(additional);
        }
    }

    static void checkWriteable(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer, int length) {
        ByteBufUtilExtension.correctSize(buffer, length, application.getBuffer());
    }

}
