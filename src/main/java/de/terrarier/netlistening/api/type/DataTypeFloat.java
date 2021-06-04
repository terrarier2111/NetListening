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

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class DataTypeFloat extends DataType<Float> {

    DataTypeFloat() {
        super((byte) 0xC, (byte) 4, true);
    }

    @AssumeNotNull
    @Override
    protected Float read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                         @AssumeNotNull ByteBuf buffer) {
        return buffer.readFloat();
    }

    @Override
    protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                         @AssumeNotNull Float data) {
        buffer.writeFloat(data);
    }

}
