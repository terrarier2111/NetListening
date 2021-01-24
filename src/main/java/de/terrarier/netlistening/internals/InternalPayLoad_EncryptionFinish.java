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
public final class InternalPayLoad_EncryptionFinish extends InternalPayload {

    protected InternalPayLoad_EncryptionFinish() {
        super((byte) 0x5);
    }

    @Override
    protected void write(@NotNull Application application, @NotNull ByteBuf buffer) {
        if(!application.isClient()) {
            throw new UnsupportedOperationException("This payload can only be sent by the client!");
        }
    }

    @Override
    public void read(@NotNull Application application, @NotNull Channel channel, @NotNull ByteBuf buffer)
            throws CancelReadingSignal {
        if(application.isClient()) {
            throw new UnsupportedOperationException("The server sent an invalid payload!");
        }
        final ConnectionImpl connection = (ConnectionImpl) application.getConnection(channel);
        connection.prepare();
    }

}
