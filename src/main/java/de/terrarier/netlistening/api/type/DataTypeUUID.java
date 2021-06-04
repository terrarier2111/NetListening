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

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class DataTypeUUID extends DataType<UUID> {

    DataTypeUUID() {
        super((byte) 0xB, (byte) 16, true);
    }

    @AssumeNotNull
    @Override
    protected UUID read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                        @AssumeNotNull ByteBuf buffer) {
        final long mostSignificantBits = buffer.readLong();
        final long leastSignificantBits = buffer.readLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    @Override
    protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                         @AssumeNotNull UUID uuid) {
        final long mostSignificantBits = uuid.getMostSignificantBits();
        final long leastSignificantBits = uuid.getLeastSignificantBits();
        buffer.writeLong(mostSignificantBits);
        buffer.writeLong(leastSignificantBits);
    }

}
