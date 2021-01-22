package de.terrarier.netlistening.api.compression;

import de.terrarier.netlistening.Server;

public final class CompressionSettingWrapper {

    private final CompressionSetting compressionSetting = new CompressionSetting();
    private final Server.Builder builder;

    public CompressionSettingWrapper(Server.Builder builder) {
        this.builder = builder;
    }

    public CompressionSettingWrapper varIntCompression(boolean enabled) {
        compressionSetting.varIntCompression(enabled);
        return this;
    }

    public CompressionSettingWrapper nibbleCompression(boolean enabled) {
        compressionSetting.nibbleCompression(enabled);
        return this;
    }

    public Server.Builder build() {
        return builder.compression(compressionSetting);
    }

}
