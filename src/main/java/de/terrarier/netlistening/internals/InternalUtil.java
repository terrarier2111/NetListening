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

import de.terrarier.netlistening.api.compression.VarIntUtil;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class InternalUtil {

    private InternalUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    public static void writeInt(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, int value) {
        ByteBufUtilExtension.correctSize(buffer, getSize(application, value), application.getBuffer());
        writeIntUnchecked(application, buffer, value);
    }

    public static void writeIntUnchecked(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                                         int value) {
        if (application.getCompressionSetting().isVarIntCompression()) {
            VarIntUtil.writeVarInt(value, buffer);
        } else {
            buffer.writeInt(value);
        }
    }

    public static int readInt(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer)
            throws CancelReadSignal {
        if (application.getCompressionSetting().isVarIntCompression()) {
            return VarIntUtil.getVarInt(buffer);
        }
        if (buffer.readableBytes() < 4) {
            throw VarIntUtil.FOUR_BYTES;
        }
        return buffer.readInt();
    }

    static int getSize(@AssumeNotNull ApplicationImpl application, int value) {
        return application.getCompressionSetting().isVarIntCompression() ? VarIntUtil.varIntSize(value) : 4;
    }

    public static int singleOctetIntSize(@AssumeNotNull ApplicationImpl application) {
        return application.getCompressionSetting().isVarIntCompression() ? 1 : 4;
    }

}
