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
package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class InternalPayloadEncryptionFinish extends InternalPayload {

    InternalPayloadEncryptionFinish() {
        super((byte) 0x4);
    }

    @Override
    void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        if (application instanceof Server) {
            throw new UnsupportedOperationException("This payload can only be sent by a client!");
        }
    }

    @Override
    public void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                     @AssumeNotNull ByteBuf buffer) {
        if (application instanceof Client) {
            throw new UnsupportedOperationException("The server sent an invalid payload!");
        }
        connection.prepare();
    }

}
