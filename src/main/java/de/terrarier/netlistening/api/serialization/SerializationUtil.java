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
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.CancelSignal;
import de.terrarier.netlistening.utils.TwoArgsBooleanFunction;
import de.terrarier.netlistening.utils.TwoArgsFunction;
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

    public static byte[] serialize(@AssumeNotNull ApplicationImpl application, @AssumeNotNull Object obj)
            throws CancelSignal {
        return performOperation(application, SerializationProvider::isSerializable, SerializationProvider::serialize,
                obj);
    }

    public static Object deserialize(@AssumeNotNull ApplicationImpl application, byte[] data) throws CancelSignal {
        return performOperation(application, SerializationProvider::isDeserializable,
                SerializationProvider::deserialize, data);
    }

    private static <A, R> R performOperation(@AssumeNotNull ApplicationImpl application,
                                             @AssumeNotNull TwoArgsBooleanFunction<SerializationProvider, A> check,
                                             @AssumeNotNull TwoArgsFunction<SerializationProvider, A, R> op, A param)
            throws CancelSignal {

        final SerializationProvider mainProvider = application.getSerializationProvider();
        SerializationProvider provider = mainProvider;
        while (provider != null) {
            if (check.apply(provider, param)) {
                try {
                    return op.apply(provider, param);
                } catch (Exception exception) {
                    mainProvider.handleException(exception);
                    return null;
                }
            } else {
                provider = provider.getFallback();
            }
        }
        mainProvider.handleException(new UnsupportedOperationException(
                "There is no serialization provider available which can perform this operation on this Object. (" +
                        param.getClass().getName() + ')'));
        throw CancelSignal.INSTANCE;
    }

}
