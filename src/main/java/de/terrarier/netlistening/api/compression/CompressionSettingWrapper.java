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
package de.terrarier.netlistening.api.compression;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class CompressionSettingWrapper {

    private final CompressionSetting compressionSetting = new CompressionSetting();
    private final Server.Builder builder;

    public CompressionSettingWrapper(@NotNull Server.Builder builder) {
        this.builder = builder;
    }

    /**
     * @see CompressionSetting#varIntCompression(boolean)
     */
    @AssumeNotNull
    public CompressionSettingWrapper varIntCompression(boolean enabled) {
        compressionSetting.varIntCompression(enabled);
        return this;
    }

    /**
     * @see CompressionSetting#nibbleCompression(boolean)
     */
    @AssumeNotNull
    public CompressionSettingWrapper nibbleCompression(boolean enabled) {
        compressionSetting.nibbleCompression(enabled);
        return this;
    }

    /**
     * Sets the compression setting for the builder
     * and returns it.
     *
     * @return the builder which was used before.
     */
    @AssumeNotNull
    public Server.Builder build() {
        return builder.compression(compressionSetting);
    }

}
