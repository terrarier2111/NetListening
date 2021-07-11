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
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;

import static de.terrarier.netlistening.internal.InternalUtil.*;

/**
 * @author Terrarier2111
 * @since 1.12
 */
@ApiStatus.Internal
public final class InternalPayloadUpdateTranslationEntry extends InternalPayload {

    private final int id;
    private final int newId;

    InternalPayloadUpdateTranslationEntry(int id) {
        this(id, -1);
    }

    public InternalPayloadUpdateTranslationEntry(int id, int newId) {
        super((byte) 0x5);
        this.id = id;
        this.newId = newId;
    }

    @Override
    void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        writeInt(application, buffer, id);
        if (newId != -1) {
            writeInt(application, buffer, newId);
        }
    }

    @Override
    void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
              @AssumeNotNull ByteBuf buffer) throws CancelReadSignal {
        final int id = readInt(application, buffer);
        if (application instanceof Server) {
            connection.getPacketIdTranslationCache().delete(id);
        } else {
            final int newId = readInt(application, buffer);
            application.getCache().swapId(id, newId);
            final ByteBuf translationUpdateBuffer = Unpooled.buffer(singleOctetIntSize(application) + 1 +
                    getSize(application, id));
            DataType.getDTIP().write(application, translationUpdateBuffer,
                    new InternalPayloadUpdateTranslationEntry(id));
            final Channel channel = connection.getChannel();
            channel.writeAndFlush(translationUpdateBuffer, channel.voidPromise());
        }
    }

}
