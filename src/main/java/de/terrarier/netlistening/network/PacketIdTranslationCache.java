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
package de.terrarier.netlistening.network;

import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internal.AssumeNotNull;
import de.terrarier.netlistening.internal.InternalPayloadUpdateTranslationEntry;
import de.terrarier.netlistening.internal.InternalUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Terrarier2111
 * @since 1.12
 */
@ApiStatus.Internal
public final class PacketIdTranslationCache {

    private final Map<Integer, Integer> translations = new ConcurrentHashMap<>();
    private final ConnectionImpl connection;
    private final ApplicationImpl application;
    private final int initSize;

    public PacketIdTranslationCache(@AssumeNotNull ConnectionImpl connection,
                                    @AssumeNotNull ApplicationImpl application) {
        this.connection = connection;
        this.application = application;
        initSize = InternalUtil.singleOctetIntSize(application) + 1 + 4 + 4; // TODO: Improve init size.
    }

    public void insert(int foreign, int local) {
        translations.put(foreign, local);
        final ByteBuf buffer = Unpooled.buffer(initSize);
        DataType.getDTIP().write0(application, buffer, new InternalPayloadUpdateTranslationEntry(foreign, local));
        final Channel channel = connection.getChannel();
        channel.writeAndFlush(buffer, channel.voidPromise());
    }

    public void delete(int foreign) {
        translations.remove(foreign);
    }

    public int tryTranslate(int id) {
        return translations.getOrDefault(id, id);
    }

}
