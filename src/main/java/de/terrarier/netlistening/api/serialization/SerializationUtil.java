package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.Application;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.01
 * @author Terrarier2111
 */
public final class SerializationUtil {

    private SerializationUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    // TODO: Probably reduce code duplication!
    public static byte[] serialize(@NotNull Application application, @NotNull Object obj) {
        final SerializationProvider mainProvider = application.getSerializationProvider();
        SerializationProvider provider = mainProvider;
        while(provider != null) {
            if(provider.isSerializable(obj)) {
                try {
                    return provider.serialize(obj);
                } catch (Exception exception) {
                    mainProvider.handleException(exception);
                    return null;
                }
            }else {
                provider = provider.getFallback();
            }
        }
        mainProvider.handleException(new UnsupportedOperationException(
                "There is no serialization provider available which can serialize this Object."));
        return null;
    }

    public static Object deserialize(@NotNull Application application, byte[] data) {
        final SerializationProvider mainProvider = application.getSerializationProvider();
        SerializationProvider provider = mainProvider;
        while(provider != null) {
            if(provider.isDeserializable(data)) {
                try {
                    return provider.deserialize(data);
                } catch (Exception exception) {
                    mainProvider.handleException(exception);
                    return null;
                }
            }else {
                provider = provider.getFallback();
            }
        }
        mainProvider.handleException(new UnsupportedOperationException(
                "There is no serialization provider available which can deserialize this Object."));
        return null;
    }

}
