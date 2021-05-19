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
package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.Connection;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is called when data is framed.
 *
 * @author Terrarier2111
 * @since 1.05
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
