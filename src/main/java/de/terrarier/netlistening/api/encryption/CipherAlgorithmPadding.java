package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum CipherAlgorithmPadding {

    NO, ISO10126, PKCS1, PKCS5, SSL3;

    private static final CipherAlgorithmPadding[] VALUES = values();

    @AssumeNotNull
    public String getPaddingName() {
        return name() + "Padding";
    }

    @AssumeNotNull
    public static CipherAlgorithmPadding fromId(byte id) {
        return VALUES[id];
    }

}
