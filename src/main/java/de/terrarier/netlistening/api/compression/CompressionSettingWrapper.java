package de.terrarier.netlistening.api.compression;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class CompressionSettingWrapper {

    private final CompressionSetting compressionSetting = new CompressionSetting();
    private final Server.Builder builder;

    public CompressionSettingWrapper(@NotNull Server.Builder builder) {
        this.builder = builder;
    }

    /**
     * @see CompressionSetting
     */
    @AssumeNotNull
    public CompressionSettingWrapper varIntCompression(boolean enabled) {
        compressionSetting.varIntCompression(enabled);
        return this;
    }

    /**
     * @see CompressionSetting
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
