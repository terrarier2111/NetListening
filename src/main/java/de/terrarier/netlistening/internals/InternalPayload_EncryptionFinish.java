package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class InternalPayload_EncryptionFinish extends InternalPayload {

    protected InternalPayload_EncryptionFinish() {
        super((byte) 0x4);
    }

    @Override
    protected void write(@NotNull Application application, @NotNull ByteBuf buffer) {
        if(!application.isClient()) {
            throw new UnsupportedOperationException("This payload can only be sent by the client!");
        }
    }

    @Override
    public void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer) {
        if(application.isClient()) {
            throw new UnsupportedOperationException("The server sent an invalid payload!");
        }
        ((ConnectionImpl) application.getConnection(channel)).prepare();
    }

}
