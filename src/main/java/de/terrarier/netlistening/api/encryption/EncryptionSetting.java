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

import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class EncryptionSetting {

    private EncryptionOptions asymmetricEncryptionSetting = new EncryptionOptions().type(CipherEncryptionAlgorithm.RSA);
    private EncryptionOptions symmetricEncryptionSetting = new EncryptionOptions().type(CipherEncryptionAlgorithm.AES);
    private HmacSetting hmacSetting = new HmacSetting();
    private AsymmetricEncryptionData asymmetricEncryptionData;

    /**
     * Sets the asymmetric encryption setting which is used for encrypting the symmetric encryption key.
     *
     * @param asymmetricEncryption the options for the symmetric encryption chosen by the user or if not present by default.
     * @return the local reference.
     */
    @AssumeNotNull
    public EncryptionSetting asymmetricEncryptionOptions(@NotNull EncryptionOptions asymmetricEncryption) {
        if (asymmetricEncryptionSetting != null) {
            asymmetricEncryption.getKeySize();
            asymmetricEncryptionSetting = asymmetricEncryption;
        }
        return this;
    }

    /**
     * Sets the symmetric encryption setting which is used for encrypting userdata.
     *
     * @param symmetricEncryption the options for the symmetric encryption chosen by the user or if not present by default.
     * @return the local reference.
     */
    @AssumeNotNull
    public EncryptionSetting symmetricEncryptionOptions(@NotNull EncryptionOptions symmetricEncryption) {
        if (symmetricEncryptionSetting != null) {
            symmetricEncryption.getKeySize();
            symmetricEncryptionSetting = symmetricEncryption;
        }
        return this;
    }

    /**
     * Sets the hmac setting which should be used to hash traffic.
     *
     * @param hmacSetting the setting which should be used to hash traffic.
     * @return the local reference.
     */
    @AssumeNotNull
    public EncryptionSetting hmac(HmacSetting hmacSetting) {
        this.hmacSetting = hmacSetting;
        return this;
    }

    /**
     * Disables hmac which by default helps securing the traffic.
     *
     * @return the local reference.
     */
    @AssumeNotNull
    public EncryptionSetting disableHmac() {
        return hmac(null);
    }

    /**
     * @return the asymmetric encryption configuration set by the user or otherwise by default.
     */
    @AssumeNotNull
    public EncryptionOptions getAsymmetricSetting() {
        return asymmetricEncryptionSetting;
    }

    /**
     * @return the symmetric encryption configuration set by the user or otherwise by default.
     */
    @AssumeNotNull
    public EncryptionOptions getSymmetricSetting() {
        return symmetricEncryptionSetting;
    }

    /**
     * @return the hmac configuration set by the user or otherwise by default.
     */
    public HmacSetting getHmacSetting() {
        return hmacSetting;
    }

    /**
     * Sets the {@code asymmetricEncryptionData} used to encrypt the symmetric key sent to the clients.
     *
     * @param key the asymmetric keys represented as a byte array.
     * @return the reference to the local reference.
     * @throws NoSuchAlgorithmException when the algorithm specified in the {@code asymmetricEncryptionSetting} isn't defined in the current JDK.
     * @throws InvalidKeySpecException  when the passed key is invalid.
     */
    @AssumeNotNull
    public EncryptionSetting init(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (key != null) {
            asymmetricEncryptionData = new AsymmetricEncryptionData(asymmetricEncryptionSetting, key);
        } else {
            asymmetricEncryptionData = AsymmetricEncryptionUtil.generate(asymmetricEncryptionSetting);
        }
        return this;
    }

    /**
     * @return the {@code asymmetricEncryptionData} which provides an asymmetric key pair.
     */
    @AssumeNotNull
    public AsymmetricEncryptionData getEncryptionData() {
        return asymmetricEncryptionData;
    }

    /**
     * @return if the {@code asymmetricEncryptionData} was already set.
     */
    public boolean isInitialized() {
        return asymmetricEncryptionData != null;
    }

}
