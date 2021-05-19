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
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public final class PacketSkeleton {

    private final int id;
    private final DataType<?>[] data;
    private final int hash;
    private volatile boolean registered;

    PacketSkeleton(int id, @AssumeNotNull DataType<?>... data) {
        this.id = id;
        this.data = data;
        hash = Arrays.hashCode(data);
    }

    public int getId() {
        return id;
    }

    @AssumeNotNull
    public DataType<?>[] getData() {
        return data;
    }

    boolean isRegistered() {
        return registered;
    }

    public void register() {
        registered = true;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hash;
    }

}
