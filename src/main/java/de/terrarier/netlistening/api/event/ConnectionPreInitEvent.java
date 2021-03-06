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

import de.terrarier.netlistening.internal.AssumeNotNull;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event gets called before a connection is established.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class ConnectionPreInitEvent extends Cancellable implements Event {

    private final Channel channel;

    @ApiStatus.Internal
    public ConnectionPreInitEvent(@AssumeNotNull Channel channel) {
        this.channel = channel;
    }

    /**
     * @return the channel, representing a connection which is about
     * to be established by the api.
     */
    @AssumeNotNull
    public Channel getChannel() {
        return channel;
    }

}
