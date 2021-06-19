/*
Copyright 2021 Terrarier2111

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
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

    /**
     * @return the size which is used by default.
     */
    public int getDefaultSize() {
        return defaultSize;
    }

    /**
     * @return the mode which is used by default.
     */
    @AssumeNotNull
    public CipherAlgorithmMode getDefaultMode() {
        return defaultMode;
    }

    /**
     * @return the padding which is used by default.
     */
    @AssumeNotNull
    public CipherAlgorithmPadding getDefaultPadding() {
        return defaultPadding;
    }

    /**
     * @return whether the algorithm is symmetric or not.
     */
    @ApiStatus.Experimental
    public boolean isSymmetric() {
        return symmetric;
    }

    /**
     * Maps an ordinal number to its respective algorithm.
     *
     * @param id the ordinal of the algorithm which should be returned.
     * @return the algorithm which has an ordinal of id.
     */
    @AssumeNotNull
    public static CipherEncryptionAlgorithm fromId(byte id) {
        return VALUES[id];
    }

}
