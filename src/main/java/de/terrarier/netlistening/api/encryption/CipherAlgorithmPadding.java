package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum CipherAlgorithmPadding {

    NO, ISO10126, PKCS1, PKCS5, SSL3;

    private static final CipherAlgorithmPadding[] VALUES = values();

    @NotNull
    public String getPaddingName() {
        return name() + "Padding";
    }

    @NotNull
    public static CipherAlgorithmPadding fromId(byte id) {
        return VALUES[id];
    }

}
