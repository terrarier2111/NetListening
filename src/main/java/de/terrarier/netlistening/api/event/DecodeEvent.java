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
import de.terrarier.netlistening.api.DataContainer;
import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is called when a packet gets decoded.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class DecodeEvent extends ConnectionEvent {

    private final DataContainer data;

    @ApiStatus.Internal
    public DecodeEvent(@AssumeNotNull Connection connection, @AssumeNotNull DataContainer data) {
        super(connection);
        this.data = data;
    }

    /**
     * @return the data which was decoded.
     */
    @AssumeNotNull
    public DataContainer getData() {
        return data;
    }

}
