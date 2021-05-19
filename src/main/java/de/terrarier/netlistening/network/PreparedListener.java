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

import de.terrarier.netlistening.api.Type;
import de.terrarier.netlistening.api.event.DecodeListener;
import de.terrarier.netlistening.api.event.Event;
import de.terrarier.netlistening.api.event.PacketListener;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class PreparedListener {

    private static final Type[] EMPTY_TYPES = new Type[0];
    private final DecodeListener wrapped;
    private final Type[] types;
    private final int hash;

    public PreparedListener(@AssumeNotNull DecodeListener listener) throws NoSuchMethodException, SecurityException {
        wrapped = listener;
        final Method method = listener.getClass().getDeclaredMethod("trigger", Event.class);
        final PacketListener packetListener = method.getAnnotation(PacketListener.class);
        types = packetListener != null ? packetListener.dataTypes() : EMPTY_TYPES;
        hash = Arrays.hashCode(types);
    }

    @AssumeNotNull
    public DecodeListener getWrapped() {
        return wrapped;
    }

    @AssumeNotNull
    public Type[] getTypes() {
        return types;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hash;
    }

}
