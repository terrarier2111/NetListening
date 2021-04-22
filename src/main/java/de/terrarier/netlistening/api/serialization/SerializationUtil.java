package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.internals.CancelSignal;
import de.terrarier.netlistening.utils.TwoArgsBooleanFunction;
import de.terrarier.netlistening.utils.TwoArgsFunction;
import org.jetbrains.annotations.ApiStatus;

/**
 * @since 1.01
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class SerializationUtil {

    private SerializationUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    public static byte[] serialize(@AssumeNotNull ApplicationImpl application, @AssumeNotNull Object obj) throws CancelSignal {
        return performOperation(application, SerializationProvider::isSerializable, SerializationProvider::serialize, obj);
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
                "There is no serialization provider available which can perform this operation on this Object. (" + param.getClass().getName() + ')'));
        throw CancelSignal.INSTANCE;
    }

}
