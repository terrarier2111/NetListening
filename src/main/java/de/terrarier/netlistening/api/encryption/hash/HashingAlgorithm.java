package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum HashingAlgorithm {

    SHA_2, SHA_3, SHA_256;

    private static final HashingAlgorithm[] VALUES = values();
    private final String rawName = name().replace('_', '-');
    private final String macName = "Hmac" + name().replaceFirst("_", "");

    // TODO: Add docs for methods!
    @AssumeNotNull
    public String getRawName() {
        return rawName;
    }

    @AssumeNotNull
    public String getMacName() {
        return macName;
    }

    /**
     * Maps an ordinal number to its respective algorithm.
     *
     * @param id the ordinal of the algorithm which should be returned.
     * @return the algorithm which has an ordinal of id.
     */
    @AssumeNotNull
    public static HashingAlgorithm fromId(byte id) {
        return VALUES[id];
    }

}
