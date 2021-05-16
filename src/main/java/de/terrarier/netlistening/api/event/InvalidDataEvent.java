package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import static io.netty.util.internal.EmptyArrays.EMPTY_BYTES;

/**
 * This event gets called when invalid data was received.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class InvalidDataEvent extends ConnectionEvent {

    private final DataInvalidReason reason;
    private final byte[] data;

    @ApiStatus.Internal
    public InvalidDataEvent(@AssumeNotNull Connection connection, @AssumeNotNull DataInvalidReason reason,
                            @AssumeNotNull byte[] data) {
        super(connection);
        this.reason = reason;
        this.data = data;
    }

    @ApiStatus.Internal
    public InvalidDataEvent(@AssumeNotNull Connection connection, @AssumeNotNull DataInvalidReason reason) {
        super(connection);
        this.reason = reason;
        this.data = EMPTY_BYTES;
    }

    /**
     * @return the reason why the data is considered invalid.
     */
    @AssumeNotNull
    public DataInvalidReason getReason() {
        return reason;
    }

    /**
     * @return the data which was detected as invalid.
     */
    @AssumeNotNull
    public byte[] getData() {
        return data;
    }

    public enum DataInvalidReason {

        EMPTY_PACKET, INCOMPLETE_PACKET, MALICIOUS_ACTION, INVALID_ID, INVALID_LENGTH, INVALID_DATA_TYPE,
        INVALID_KEEP_ALIVE_ID, INVALID_HANDSHAKE, UNSPECIFIED

    }

}
