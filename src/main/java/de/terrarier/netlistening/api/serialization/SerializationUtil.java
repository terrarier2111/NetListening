package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.utils.TwoArgsBooleanFunction;
import de.terrarier.netlistening.utils.TwoArgsFunction;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.01
 * @author Terrarier2111
 */
public final class SerializationUtil {

    private SerializationUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    public static byte[] serialize(@NotNull Application application, @NotNull Object obj) {
        return performOperation(application, SerializationProvider::isSerializable, SerializationProvider::serialize, obj);
    }

    public static Object deserialize(@NotNull Application application, byte[] data) {
        return performOperation(application, SerializationProvider::isDeserializable, SerializationProvider::deserialize, data);
    }

    private static <A, R> R performOperation(@NotNull Application application,
                                             @NotNull TwoArgsBooleanFunction<SerializationProvider, A> check,
                                             @NotNull TwoArgsFunction<SerializationProvider, A, R> op, A param) {

        final SerializationProvider mainProvider = application.getSerializationProvider();
        SerializationProvider provider = mainProvider;
        while(provider != null) {
            if(check.apply(provider, param)) {
                try {
                    return op.apply(provider, param);
                } catch (Exception exception) {
                    mainProvider.handleException(exception);
                    return null;
                }
            }else {
                provider = provider.getFallback();
            }
        }
        mainProvider.handleException(new UnsupportedOperationException(
                "There is no serialization provider available which can perform this operation on this Object."));
        return null;
    }

}
