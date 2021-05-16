package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public enum CipherAlgorithmPadding {

    NO, ISO10126, PKCS1, PKCS5, SSL3;

    private static final CipherAlgorithmPadding[] VALUES = values();

    /**
     * @return the padding name used as part of the parameter passed to
     * {@code javax.crypto.Cipher#getInstance(String)}.
     */
    @AssumeNotNull
    public String getPaddingName() {
        return name() + "Padding";
    }

    /**
     * Maps an ordinal number to its respective padding.
     *
     * @param id the ordinal of the padding which should be returned.
     * @return the padding which has an ordinal of id.
     */
    @AssumeNotNull
    public static CipherAlgorithmPadding fromId(byte id) {
        return VALUES[id];
    }

}
