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
package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.api.encryption.CipherEncryptionAlgorithm;
import de.terrarier.netlistening.api.encryption.EncryptionOptions;
import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class HmacSetting {

    private HashingAlgorithm hashingAlgorithm = HashingAlgorithm.SHA_256;
    private EncryptionOptions encryptionOptions = new EncryptionOptions().type(CipherEncryptionAlgorithm.AES);
    private HmacApplicationPolicy applicationPolicy = HmacApplicationPolicy.ENCRYPTED;

    /**
     * Sets the hashing algorithm which should be used to calculate the hmac of messages.
     *
     * @param hashingAlgorithm the hashing algorithm which should be used to calculate the hmac of messages.
     * @return the local reference.
     */
    @AssumeNotNull
    public HmacSetting hashingAlgorithm(@NotNull HashingAlgorithm hashingAlgorithm) {
        this.hashingAlgorithm = hashingAlgorithm;
        return this;
    }

    /**
     * Sets the symmetric encryption setting which is to be used to calculate the hmac of messages.
     *
     * @param encryption the options for the hmac chosen by the user or if not present by default.
     * @return the local reference.
     */
    @AssumeNotNull
    public HmacSetting encryptionOptions(@NotNull EncryptionOptions encryption) {
        encryption.getKeySize(); // This line makes sure that checkBuilt was called before any get ops could be performed.
        this.encryptionOptions = encryption;
        return this;
    }

    /**
     * Specifies in which case an hmac should be sent alongside the traffic.
     *
     * @param applicationPolicy the application policy in which an hmac should be sent alongside the traffic.
     * @return the local reference.
     */
    @AssumeNotNull
    public HmacSetting applicationPolicy(@NotNull HmacApplicationPolicy applicationPolicy) {
        this.applicationPolicy = applicationPolicy;
        return this;
    }

    /**
     * @return the hashing algorithm set by the user or, if not present,
     * the hashing algorithm specified by default.
     */
    @AssumeNotNull
    public HashingAlgorithm getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    /**
     * @return the encryption configuration set by the user or, if not present,
     * the encryption configuration specified by default.
     */
    @AssumeNotNull
    public EncryptionOptions getEncryptionSetting() {
        return encryptionOptions;
    }

    /**
     * @return the application policy that specifies in which cases the hmac should be applied
     * to the traffic and therefore should be sent alongside the traffic.
     */
    @AssumeNotNull
    public HmacApplicationPolicy getApplicationPolicy() {
        return applicationPolicy;
    }

}
