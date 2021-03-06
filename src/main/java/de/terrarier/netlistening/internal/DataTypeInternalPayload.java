/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.internal;

import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.InvalidDataEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class DataTypeInternalPayload extends DataType<InternalPayload> {

    public DataTypeInternalPayload() {
        super((byte) 0x0, (byte) 1, false);
    }

    @Override
    public InternalPayload read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                                @AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        final byte payloadId = buffer.readByte();
        try {
            InternalPayload.fromId(payloadId).read(application, connection, buffer);
        } catch (IllegalStateException e) {
            final InvalidDataEvent event = new InvalidDataEvent(connection,
                    InvalidDataEvent.DataInvalidReason.INVALID_PAYLOAD_TYPE, new byte[]{payloadId});
            if (application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    event)) {
                return null;
            }
            throw e;
        }
        return null;
    }

    @Override
    public void write0(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                       @AssumeNotNull ByteBuf buffer, @AssumeNotNull InternalPayload data) {
        write(application, buffer, data);
    }

    @Override
    public void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                      @AssumeNotNull InternalPayload data) {
        data.write0(application, buffer);
    }

}
