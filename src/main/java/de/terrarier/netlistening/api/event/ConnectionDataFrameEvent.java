package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is called when data is framed.
 *
 * @since 1.05
 * @author Terrarier2111
 */
public final class ConnectionDataFrameEvent extends ConnectionEvent {

    private final int frameBytes;
    private final int readBytes;

    @ApiStatus.Internal
    public ConnectionDataFrameEvent(@AssumeNotNull Connection connection, int frameBytes, int readBytes) {
        super(connection);
        this.frameBytes = frameBytes;
        this.readBytes = readBytes;
    }

    /**
     * @return The number of bytes which should be framed.
     */
    public int getFrameBytes() {
        return frameBytes;
    }

    /**
     * @return The number of bytes which were already read of this packet part.
     */
    public int getReadBytes() {
        return readBytes;
    }

}
