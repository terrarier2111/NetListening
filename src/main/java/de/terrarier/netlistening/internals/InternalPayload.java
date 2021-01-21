package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public abstract class InternalPayload {

    private static final InternalPayLoad_RegisterInPacket REGISTER_IN_PACKET = new InternalPayLoad_RegisterInPacket();
    private static final InternalPayLoad_RegisterOutPacket REGISTER_OUT_PACKET = new InternalPayLoad_RegisterOutPacket();
    public static final InternalPayLoad_Handshake HANDSHAKE = new InternalPayLoad_Handshake();
    protected static final InternalPayload_EncryptionInit ENCRYPTION_INIT = new InternalPayload_EncryptionInit();
    protected static final InternalPayLoad_EncryptionFinish ENCRYPTION_FINISH = new InternalPayLoad_EncryptionFinish();

    private final byte id;

    protected InternalPayload(byte id) {
        this.id = id;
    }

    public final void write0(@NotNull Application application, @NotNull ByteBuf buffer) {
        checkWriteable(application, buffer, 1); // TODO: Check if there's a better solution
        buffer.writeByte(id);
        write(application, buffer);
    }

    public final byte getId() {
        return id;
    }

    protected abstract void write(@NotNull Application application, @NotNull ByteBuf buffer);

    public abstract void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
            throws CancelReadingSignal;

    @NotNull
    public static InternalPayload fromId(byte id) throws IllegalArgumentException {
        switch (id) {
            case 0x1:
                return REGISTER_IN_PACKET;
            case 0x2:
                return REGISTER_OUT_PACKET;
            case 0x3:
                return HANDSHAKE;
            case 0x4:
                return ENCRYPTION_INIT;
            case 0x5:
                return ENCRYPTION_FINISH;
            default:
                throw new IllegalArgumentException("Tried to process an InternalPayload with an invalid id! (" + Integer.toHexString(id) + ")");
        }
    }

    protected final void checkReadable(@NotNull ByteBuf buffer, int required, int additional)
            throws CancelReadingSignal {
        if (buffer.readableBytes() < additional) {
            buffer.readerIndex(buffer.readerIndex() - required);
            throw new CancelReadingSignal(required + additional);
        }
    }

    protected final void checkWriteable(@NotNull Application application, @NotNull ByteBuf buffer, int length) {
        ByteBufUtilExtension.correctSize(buffer, length, application.getBuffer());
    }

}
