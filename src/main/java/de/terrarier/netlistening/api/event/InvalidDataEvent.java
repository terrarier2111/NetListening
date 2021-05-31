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
        data = EMPTY_BYTES;
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
        INVALID_KEEP_ALIVE_ID, INVALID_HANDSHAKE, TOO_LARGE_FRAME, UNSPECIFIED

    }

}
