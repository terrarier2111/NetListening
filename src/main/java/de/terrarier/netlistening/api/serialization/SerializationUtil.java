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
package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.internal.CancelSignal;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.01
 */
@ApiStatus.Internal
public final class SerializationUtil {

    private SerializationUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    public static void serialize(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                                 @AssumeNotNull Object obj) throws CancelSignal {
        final SerializationProvider mainProvider = application.getSerializationProvider();
        SerializationProvider provider = mainProvider;
        while (provider != null) {
            if (provider.isSerializable(obj)) {
                final WritableByteAccumulation ba = new WritableByteAccumulation(application, buffer);
                try {
                    provider.serialize(ba, obj);
                    ba.updateLength();
                } catch (Exception exception) {
                    ba.rollback();
                    mainProvider.handleException(exception);
                }
                return;
            }
            provider = provider.getFallback0();
        }
        // Sending an empty object in order to be able to proceed encoding.
        buffer.writeInt(0);
        mainProvider.handleException(new UnsupportedOperationException(
                "There is no serialization provider available which can serialize this Object. (" +
                        obj.getClass().getName() + ')'));
        throw CancelSignal.INSTANCE;
    }

    public static Object deserialize(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                                     int length) throws CancelSignal {
        final SerializationProvider mainProvider = application.getSerializationProvider();
        SerializationProvider provider = mainProvider;
        final ReadableByteAccumulation ba = new ReadableByteAccumulation(buffer, length);
        while (provider != null) {
            if (provider.isDeserializable(ba)) {
                try {
                    return provider.deserialize(ba);
                } catch (Exception exception) {
                    mainProvider.handleException(exception);
                }
                return null;
            }
            provider = provider.getFallback0();
        }
        mainProvider.handleException(new UnsupportedOperationException(
                "There is no serialization provider available which can deserialize this Object."));
        throw CancelSignal.INSTANCE;
    }

    public static void init(@AssumeNotNull ApplicationImpl application,
                            @AssumeNotNull SerializationProvider serializationProvider) {
        serializationProvider.setEventManager(application.getEventManager());
    }

}
