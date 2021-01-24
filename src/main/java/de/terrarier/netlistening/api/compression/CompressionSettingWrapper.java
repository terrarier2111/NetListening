package de.terrarier.netlistening.api.compression;

import de.terrarier.netlistening.Server;
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
    @NotNull
    public CompressionSettingWrapper varIntCompression(boolean enabled) {
        compressionSetting.varIntCompression(enabled);
        return this;
    }

    /**
     * @see CompressionSetting
     */
    @NotNull
    public CompressionSettingWrapper nibbleCompression(boolean enabled) {
        compressionSetting.nibbleCompression(enabled);
        return this;
    }

    @NotNull
    public Server.Builder build() {
        return builder.compression(compressionSetting);
    }

}
