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

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.InvalidDataEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;

import static io.netty.util.internal.EmptyArrays.EMPTY_BYTES;

/**
 * @author Terrarier2111
 * @since 1.12
 */
public final class InternalPayloadPushRequest extends InternalPayload {

    InternalPayloadPushRequest() {
        super((byte) 0x4);
    }

    @Override
    void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        // NOOP
    }

    @Override
    void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
              @AssumeNotNull ByteBuf buffer) {
        if (application instanceof Server) {

            final InvalidDataEvent event = new InvalidDataEvent(connection,
                    InvalidDataEvent.DataInvalidReason.MALICIOUS_ACTION, EMPTY_BYTES);
            if (application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    event)) {
                return;
            }

            throw new IllegalStateException("Received malicious data! (" + Integer.toHexString(id) + ')');
        }

        ((ClientImpl) application).pushCachedData();
    }

}
