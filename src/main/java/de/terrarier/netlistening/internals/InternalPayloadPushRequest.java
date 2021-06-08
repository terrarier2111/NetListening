package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.InvalidDataEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.utils.ConversionUtil;
import io.netty.buffer.ByteBuf;

public final class InternalPayloadPushRequest extends InternalPayload {

    public InternalPayloadPushRequest() {
        super((byte) 0x5);
    }

    @Override
    void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        // NOOP
    }

    @Override
    void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
              @AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        if (application instanceof Server) {
            // TODO: We should probably cache this byte array.
            final byte[] data = ConversionUtil.intToBytes(id);

            final InvalidDataEvent event = new InvalidDataEvent(connection,
                    InvalidDataEvent.DataInvalidReason.MALICIOUS_ACTION, data);
            if (application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    event)) {
                return;
            }

            throw new IllegalStateException("Received malicious data! (" + Integer.toHexString(id) + ")");
        }

        ((ClientImpl) application).pushCachedData();
    }

}
