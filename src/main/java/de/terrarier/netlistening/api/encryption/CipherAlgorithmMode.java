package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public enum CipherAlgorithmMode {

    NONE, CBC, CCM, CFB, CFBx, CTR, CTS, ECB, GCM, OFB, OFBx, PCBC;

    private static final CipherAlgorithmMode[] VALUES = values();

    @NotNull
    public static CipherAlgorithmMode fromId(byte id) {
        return VALUES[id];
    }

}
