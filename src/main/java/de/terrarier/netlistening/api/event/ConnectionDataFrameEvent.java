package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when data is framed.
 *
 * @since 1.05
 * @author Terrarier2111
 */
public final class ConnectionDataFrameEvent extends ConnectionEvent {

    private final int frameSize;

    public ConnectionDataFrameEvent(@NotNull Connection connection, int frameSize) {
        super(connection);
        this.frameSize = frameSize;
    }

    public int getFrameSize() {
        return frameSize;
    }

}
