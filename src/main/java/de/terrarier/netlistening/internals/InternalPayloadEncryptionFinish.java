package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

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
    void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        if(application instanceof Server) {
            throw new UnsupportedOperationException("This payload can only be sent by a client!");
        }
    }

    @Override
    public void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                     @AssumeNotNull ByteBuf buffer) {
        if(application instanceof Client) {
            throw new UnsupportedOperationException("The server sent an invalid payload!");
        }
        connection.prepare();
    }

}
