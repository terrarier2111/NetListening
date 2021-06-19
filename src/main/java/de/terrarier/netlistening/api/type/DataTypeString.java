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
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.internal.CancelReadSignal;
import de.terrarier.netlistening.util.ByteBufUtilExtension;
import de.terrarier.netlistening.util.ConversionUtil;
import io.netty.buffer.ByteBuf;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class DataTypeString extends DataType<String> {

    private static final String EMPTY_STRING = "";

    DataTypeString() {
        super((byte) 0x7, (byte) 4, true);
    }

    @AssumeNotNull
    @Override
    protected String read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                          @AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        final int length = buffer.readInt();

        if (length < 1) {
            if (length == 0) {
                return EMPTY_STRING;
            }
            final byte[] data = new byte[]{0x7, 0x0, 0x0, 0x0, 0x0};
            ConversionUtil.intToBytes(data, 1, length);
            final InvalidDataEvent event = new InvalidDataEvent(connection,
                    InvalidDataEvent.DataInvalidReason.INVALID_LENGTH, data);

            if (application.getEventManager().callEvent(ListenerType.INVALID_DATA, EventManager.CancelAction.IGNORE,
                    event)) {
                return EMPTY_STRING;
            }

            throw new IllegalStateException("Received a malicious string of length " + length + '.');
        }

        checkReadable(buffer, length);

        final byte[] bytes = ByteBufUtilExtension.readBytes(buffer, length);
        return new String(bytes, application.getStringEncoding());
    }

    @Override
    protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                         @AssumeNotNull String data) {
        ByteBufUtilExtension.writeBytes(buffer, data.getBytes(application.getStringEncoding()),
                application.getBuffer());
    }

}
