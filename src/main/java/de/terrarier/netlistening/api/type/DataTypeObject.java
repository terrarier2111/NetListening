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
package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.api.event.EventManager;
import de.terrarier.netlistening.api.event.InvalidDataEvent;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.api.serialization.SerializationUtil;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.CancelSignal;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import de.terrarier.netlistening.utils.ConversionUtil;
import io.netty.buffer.ByteBuf;

import static de.terrarier.netlistening.api.serialization.SerializationProvider.SERIALIZATION_ERROR;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class DataTypeObject extends DataType<Object> {

    DataTypeObject() {
        super((byte) 0x8, (byte) 4, true);
    }

    @Override
    protected Object read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                          @AssumeNotNull ByteBuf buffer) throws CancelSignal {
        final int length = buffer.readInt();

        if (length < 1) {
            if (length == 0) { // Occurs when an object can't get serialized properly.
                return SERIALIZATION_ERROR;
            }
            final byte[] data = new byte[]{0x8, 0x0, 0x0, 0x0, 0x0};
            ConversionUtil.intToBytes(data, 1, length);
            final InvalidDataEvent event = new InvalidDataEvent(connection,
                    InvalidDataEvent.DataInvalidReason.INVALID_LENGTH, data);

            if (application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    event)) {
                return SERIALIZATION_ERROR;
            }

            throw new IllegalStateException("Received a malicious object of length " + length + '.');
        }
        checkReadable(buffer, length);

        final byte[] bytes = ByteBufUtilExtension.readBytes(buffer, length);
        final Object deserialized = SerializationUtil.deserialize(application, bytes);

        if (deserialized == null) {
            return SERIALIZATION_ERROR;
        }

        return deserialized;
    }

    @Override
    protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                         @AssumeNotNull Object data) throws CancelSignal {
        final byte[] serialized = SerializationUtil.serialize(application, data);
        if (serialized == null) {
            // Sending an empty object in order to be able to proceed encoding!
            buffer.writeInt(0);
            return;
        }
        ByteBufUtilExtension.writeBytes(buffer, serialized, application.getBuffer());
    }

}
