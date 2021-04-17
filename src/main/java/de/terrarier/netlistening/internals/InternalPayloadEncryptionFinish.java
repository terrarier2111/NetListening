package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
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
        if(application instanceof Server) {
            throw new UnsupportedOperationException("This payload can only be sent by the client!");
        }
    }

    @Override
    public void read(@NotNull ApplicationImpl application, @NotNull ConnectionImpl connection, @NotNull ByteBuf buffer) {
        if(application instanceof Client) {
            throw new UnsupportedOperationException("The server sent an invalid payload!");
        }
        connection.prepare();
    }

}
