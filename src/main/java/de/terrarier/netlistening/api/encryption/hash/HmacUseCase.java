package de.terrarier.netlistening.api.encryption.hash;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum HmacUseCase {

    ALL, ENCRYPTED;

    private static final HmacUseCase[] VALUES = values();

    @NotNull
    public static HmacUseCase fromId(byte id) {
        return VALUES[id];
    }

}
