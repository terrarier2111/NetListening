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
package de.terrarier.netlistening.api.proxy;

import de.terrarier.netlistening.internal.AssumeNotNull;
import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.SocketAddress;

/**
 * @author Terrarier2111
 * @since 1.0
 */
final class Socks5Proxy extends Proxy {

    Socks5Proxy(@AssumeNotNull SocketAddress address) {
        super(address);
    }

    @AssumeNotNull
    @Override
    public ChannelHandler newHandler() {
        return new Socks5ProxyHandler(address);
    }

}
