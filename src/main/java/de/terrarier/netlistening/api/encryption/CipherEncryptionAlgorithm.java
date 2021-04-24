package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum CipherEncryptionAlgorithm {

    AES(128, CipherAlgorithmMode.ECB, CipherAlgorithmPadding.PKCS5, true),
    @Deprecated DES(56, CipherAlgorithmMode.ECB, CipherAlgorithmPadding.PKCS5, true),
    @Deprecated DESede(168, CipherAlgorithmMode.ECB, CipherAlgorithmPadding.PKCS5, true),
    RSA(2048, CipherAlgorithmMode.ECB, CipherAlgorithmPadding.PKCS1, false);

    private static final CipherEncryptionAlgorithm[] VALUES = values();
    private final int defaultSize;
    private final CipherAlgorithmMode defaultMode;
    private final CipherAlgorithmPadding defaultPadding;
    private final boolean symmetric;

    CipherEncryptionAlgorithm(int defaultSize, @AssumeNotNull CipherAlgorithmMode defaultMode,
                              @AssumeNotNull CipherAlgorithmPadding defaultPadding, boolean symmetric) {
        this.defaultSize = defaultSize;
        this.defaultMode = defaultMode;
        this.defaultPadding = defaultPadding;
        this.symmetric = symmetric;
    }

    // TODO: Add doc for methods.
    public int getDefaultSize() {
        return defaultSize;
    }

    @AssumeNotNull
    public CipherAlgorithmMode getDefaultMode() {
        return defaultMode;
    }

    @AssumeNotNull
    public CipherAlgorithmPadding getDefaultPadding() {
        return defaultPadding;
    }

    public boolean isSymmetric() {
        return symmetric;
    }

    @AssumeNotNull
    public static CipherEncryptionAlgorithm fromId(byte id) {
        return VALUES[id];
    }

}
