package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum CipherEncryptionAlgorithm {

    AES(128, CipherAlgorithmMode.ECB, CipherAlgorithmPadding.PKCS5, true),
    DES(56, CipherAlgorithmMode.ECB, CipherAlgorithmPadding.PKCS5, true),
    DESede(168, CipherAlgorithmMode.ECB, CipherAlgorithmPadding.PKCS5, true),
    RSA(2048, CipherAlgorithmMode.ECB, CipherAlgorithmPadding.PKCS1, false);

    private static final CipherEncryptionAlgorithm[] VALUES = values();
    private final int defaultSize;
    private final CipherAlgorithmMode defaultMode;
    private final CipherAlgorithmPadding defaultPadding;
    private final boolean symmetric;

    CipherEncryptionAlgorithm(int defaultSize, @NotNull CipherAlgorithmMode defaultMode, @NotNull CipherAlgorithmPadding defaultPadding, boolean symmetric) {
        this.defaultSize = defaultSize;
        this.defaultMode = defaultMode;
        this.defaultPadding = defaultPadding;
        this.symmetric = symmetric;
    }

    public int getDefaultSize() {
        return defaultSize;
    }

    @NotNull
    public CipherAlgorithmMode getDefaultMode() {
        return defaultMode;
    }

    @NotNull
    public CipherAlgorithmPadding getDefaultPadding() {
        return defaultPadding;
    }

    public boolean isSymmetric() {
        return symmetric;
    }

    @NotNull
    public static CipherEncryptionAlgorithm fromId(byte id) {
        return VALUES[id];
    }

}
