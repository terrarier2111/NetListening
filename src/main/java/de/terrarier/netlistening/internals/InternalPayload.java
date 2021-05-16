package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public abstract class InternalPayload {

    private static final InternalPayloadRegisterPacket REGISTER_PACKET = new InternalPayloadRegisterPacket(-1);
    public static final InternalPayloadHandshake HANDSHAKE = new InternalPayloadHandshake();
    static final InternalPayloadEncryptionInit ENCRYPTION_INIT = new InternalPayloadEncryptionInit();
    static final InternalPayloadEncryptionFinish ENCRYPTION_FINISH = new InternalPayloadEncryptionFinish();

    private final byte id;

    InternalPayload(byte id) {
        this.id = id;
    }

    final void write0(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        checkWriteable(application, buffer, 1);
        buffer.writeByte(id);
        write(application, buffer);
    }

    abstract void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer);

    public abstract void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                              @AssumeNotNull ByteBuf buffer) throws CancelReadSignal;

    @AssumeNotNull
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
                // TODO: Call invalid data event here!
                throw new IllegalStateException("Tried to process an internal payload with an invalid id! (" +
                        Integer.toHexString(id) + ')');
        }
    }

    static void checkReadable(@AssumeNotNull ByteBuf buffer, int additional) throws CancelReadSignal {
        if (buffer.readableBytes() < additional) {
            throw new CancelReadSignal(additional);
        }
    }

    static void checkWriteable(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, int length) {
        ByteBufUtilExtension.correctSize(buffer, length, application.getBuffer());
    }

}
