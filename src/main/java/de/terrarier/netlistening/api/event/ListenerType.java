package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@ApiStatus.Internal
public enum ListenerType {

    PRE_INIT(ConnectionPreInitListener.class), POST_INIT(ConnectionPostInitListener.class),
    DECODE(DecodeListener.class), TIMEOUT(ConnectionTimeoutListener.class),
    DISCONNECT(ConnectionDisconnectListener.class), INVALID_DATA(InvalidDataListener.class),
    KEY_CHANGE(KeyChangeListener.class), EXCEPTION_THROW(ExceptionThrowListener.class),
    LENGTH_EXTENSION_DETECTION(LengthExtensionDetectionListener.class), FRAME(ConnectionDataFrameEvent.class),
    NONE(null);

    static final ListenerType[] VALUES = values();
    private final Class<?> type;

    ListenerType(Class<?> type) {
        this.type = type;
    }

    static ListenerType resolveType(@AssumeNotNull Class<?> clazz) {
        for (Class<?> implemented : clazz.getInterfaces()) {
            for (ListenerType type : VALUES) {
                final Class<?> listenerType = type.type;
                if (listenerType != null && implemented == listenerType) {
                    return type;
                }
            }
        }
        return null;
    }


}
