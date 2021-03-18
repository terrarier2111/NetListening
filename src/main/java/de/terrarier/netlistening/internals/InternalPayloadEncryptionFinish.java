package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class InternalPayloadEncryptionFinish extends InternalPayload {

    InternalPayloadEncryptionFinish() {
        super((byte) 0x4);
    }

    @Override
    void write(@NotNull ApplicationImpl application, @NotNull ByteBuf buffer) {
        if(!application.isClient()) {
            throw new UnsupportedOperationException("This payload can only be sent by the client!");
        }
    }

    @Override
    public void read(@NotNull ApplicationImpl application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
        if(application.isClient()) {
            throw new UnsupportedOperationException("The server sent an invalid payload!");
        }
        ((ConnectionImpl) application.getConnection(channel)).prepare();
    }

}