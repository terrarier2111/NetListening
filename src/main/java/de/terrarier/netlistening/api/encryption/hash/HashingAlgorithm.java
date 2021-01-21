package de.terrarier.netlistening.api.encryption.hash;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum HashingAlgorithm {

    SHA_2("SHA-2"), SHA_3("SHA-3"), SHA_256("SHA-256");

    private static final HashingAlgorithm[] VALUES = values();
    private final String rawName;

    HashingAlgorithm(@NotNull String rawName) {
        this.rawName = rawName;
    }

    @NotNull
    public String getRawName() {
        return rawName;
    }

    @NotNull
    public static HashingAlgorithm fromId(byte id) {
        return VALUES[id];
    }

}
