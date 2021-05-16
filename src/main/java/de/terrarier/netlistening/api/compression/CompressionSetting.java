package de.terrarier.netlistening.api.compression;

import de.terrarier.netlistening.internals.AssumeNotNull;

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
