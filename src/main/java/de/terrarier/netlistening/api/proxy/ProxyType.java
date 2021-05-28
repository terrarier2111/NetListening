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

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import java.net.SocketAddress;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public enum ProxyType {

    SOCKS4, SOCKS5;

    @ApiStatus.Internal
    @AssumeNotNull
    public Proxy getInstance(@AssumeNotNull SocketAddress address) {
        switch (this) {
            case SOCKS4:
                return new Socks4Proxy(address);
            case SOCKS5:
                return new Socks5Proxy(address);
            default:
                return null;
        }
    }

}
