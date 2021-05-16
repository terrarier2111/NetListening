package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @author Terrarier2111
 * @since 1.08
 */
public enum HmacApplicationPolicy {

    ALL, ENCRYPTED;

    private static final HmacApplicationPolicy[] VALUES = values();

    /**
     * Maps an ordinal number to its respective application policy.
     *
     * @param id the ordinal of the application policy which should be returned.
     * @return the application policy which has an ordinal of id.
     */
    @AssumeNotNull
    public static HmacApplicationPolicy fromId(byte id) {
        return VALUES[id];
    }

}
