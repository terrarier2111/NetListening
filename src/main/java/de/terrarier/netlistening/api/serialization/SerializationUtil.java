package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.Application;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.01
 * @author Terrarier2111
 */
public final class SerializationUtil {

    private SerializationUtil() {}

    public static byte[] serialize(@NotNull Application application, @NotNull Object obj) {
        SerializationProvider provider = application.getSerializationProvider();
        while(provider != null) {
            if(provider.isSerializable(obj)) {
                try {
                    return provider.serialize(obj);
                } catch (Exception exception) {
                    application.getSerializationProvider().handleException(exception);
                    return null;
                }
            }else {
                provider = provider.getFallback();
            }
        }
        return null;
    }

    public static Object deserialize(@NotNull Application application, byte[] data) {
        SerializationProvider provider = application.getSerializationProvider();
        while(provider != null) {
            if(provider.isDeserializable(data)) {
                try {
                    return provider.deserialize(data);
                } catch (Exception exception) {
                    application.getSerializationProvider().handleException(exception);
                    return null;
                }
            }else {
                provider = provider.getFallback();
            }
        }
        return null;
    }

}
