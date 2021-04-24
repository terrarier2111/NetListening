package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum HmacUseCase {

    ALL, ENCRYPTED;

    private static final HmacUseCase[] VALUES = values();

    // TODO: Add a doc.
    @AssumeNotNull
    public static HmacUseCase fromId(byte id) {
        return VALUES[id];
    }

}
