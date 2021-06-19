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
import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event gets called when a connection times out.
 * Note that in order for this event to be called a
 * timeout has to be defined in the builder of the
 * Application.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class ConnectionTimeoutEvent extends ConnectionEvent {

    @ApiStatus.Internal
    public ConnectionTimeoutEvent(@AssumeNotNull Connection connection) {
        super(connection);
    }

}
