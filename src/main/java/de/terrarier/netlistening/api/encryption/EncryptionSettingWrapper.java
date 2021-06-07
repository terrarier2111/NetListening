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

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.encryption.hash.HmacSettingWrapper;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class EncryptionSettingWrapper extends EncryptionOptionsSuperBuilder<EncryptionSettingWrapper> {

    private final EncryptionSetting encryptionSetting = new EncryptionSetting();
    private final Server.Builder builder;

    public EncryptionSettingWrapper(@NotNull Server.Builder builder) {
        this.builder = builder;
    }

    /**
     * @see EncryptionSetting#asymmetricEncryptionOptions(EncryptionOptions)
     */
    @AssumeNotNull
    public EncryptionSettingWrapper asymmetricEncryptionOptions(@NotNull EncryptionOptions asymmetricEncryption) {
        encryptionSetting.asymmetricEncryptionOptions(asymmetricEncryption);
        return this;
    }

    /**
     * @see EncryptionSetting
     */
    @AssumeNotNull
    public EncryptionOptionsWrapper<EncryptionSettingWrapper> asymmetricEncryptionOptions() {
        return new EncryptionOptionsWrapper<>(this);
    }

    /**
     * @see EncryptionSetting#symmetricEncryptionOptions(EncryptionOptions)
     */
    @AssumeNotNull
    public EncryptionOptionsWrapper<EncryptionSettingWrapper> symmetricEncryptionOptions(
            @NotNull EncryptionOptions symmetricEncryption) {
        return new EncryptionOptionsWrapper<>(this, symmetricEncryption);
    }

    /**
     * @see EncryptionSetting
     */
    @AssumeNotNull
    public EncryptionOptionsWrapper<EncryptionSettingWrapper> symmetricEncryptionOptions() {
        return new EncryptionOptionsWrapper<>(this);
    }

    /**
     * @see EncryptionSetting#hmac(HmacSetting)
     */
    @AssumeNotNull
    public EncryptionSettingWrapper hmac(HmacSetting hmacSetting) {
        encryptionSetting.hmac(hmacSetting);
        return this;
    }

    /**
     * Creates a hmac setting wrapper which can be used to
     * adjust the hmac settings.
     *
     * @return a hmac setting wrapper.
     */
    @AssumeNotNull
    public HmacSettingWrapper hmac() {
        return new HmacSettingWrapper(this);
    }

    /**
     * @see EncryptionSetting#disableHmac()
     */
    @AssumeNotNull
    public EncryptionSettingWrapper disableHmac() {
        return hmac(null);
    }

    /**
     * Sets the encryption setting for the builder
     * and returns it.
     *
     * @return the builder which was used before.
     */
    @AssumeNotNull
    public Server.Builder build() {
        return builder.encryption(encryptionSetting);
    }

    /**
     * @see EncryptionOptionsSuperBuilder#encryptionOptions(EncryptionOptions)
     */
    @AssumeNotNull
    @Override
    protected EncryptionSettingWrapper encryptionOptions(@NotNull EncryptionOptions encryption) {
        return asymmetricEncryptionOptions(encryption);
    }

}
