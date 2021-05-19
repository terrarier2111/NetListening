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

/**
 * @author Terrarier2111
 * @since 1.0
 */
public abstract class ConnectionEvent extends Cancellable implements Event {

    private final Connection connection;

    ConnectionEvent(@AssumeNotNull Connection connection) {
        this.connection = connection;
    }

    /**
     * @return the connection this event is related to.
     */
    @AssumeNotNull
    public final Connection getConnection() {
        return connection;
    }

}
