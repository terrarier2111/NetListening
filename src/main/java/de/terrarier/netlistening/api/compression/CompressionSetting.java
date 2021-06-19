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

import de.terrarier.netlistening.internal.AssumeNotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class CompressionSetting {

    private boolean varIntCompression;
    private boolean nibbleCompression;

    /**
     * Sets if VarInt compression should be used to compress internal data
     * like packet ids.
     *
     * @param enabled if VarInt compression should be used.
     * @return the local reference.
     */
    @AssumeNotNull
    public CompressionSetting varIntCompression(boolean enabled) {
        this.varIntCompression = enabled;
        return this;
    }

    /**
     * Sets if nibble compression should be used to compress data type ids.
     *
     * @param enabled if nibble compression should be used.
     * @return the local reference.
     */
    @AssumeNotNull
    public CompressionSetting nibbleCompression(boolean enabled) {
        this.nibbleCompression = enabled;
        return this;
    }

    /**
     * @return if VarInt compression is enabled.
     */
    public boolean isVarIntCompression() {
        return varIntCompression;
    }

    /**
     * @return if nibble compression is enabled.
     */
    public boolean isNibbleCompression() {
        return nibbleCompression;
    }

}
