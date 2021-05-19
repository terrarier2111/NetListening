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

import de.terrarier.netlistening.api.encryption.EncryptionOptions;
import de.terrarier.netlistening.api.encryption.EncryptionOptionsSuperBuilder;
import de.terrarier.netlistening.api.encryption.EncryptionOptionsWrapper;
import de.terrarier.netlistening.api.encryption.EncryptionSettingWrapper;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class HmacSettingWrapper extends EncryptionOptionsSuperBuilder<HmacSettingWrapper> {

    private final HmacSetting hmacSetting = new HmacSetting();
    private final EncryptionSettingWrapper builder;

    public HmacSettingWrapper(@NotNull EncryptionSettingWrapper builder) {
        this.builder = builder;
    }

    /**
     * @see HmacSetting#hashingAlgorithm(HashingAlgorithm)
     */
    @AssumeNotNull
    public HmacSettingWrapper hashingAlgorithm(@NotNull HashingAlgorithm hashingAlgorithm) {
        hmacSetting.hashingAlgorithm(hashingAlgorithm);
        return this;
    }

    /**
     * @see HmacSetting#encryptionOptions(EncryptionOptions)
     */
    @AssumeNotNull
    @Override
    public HmacSettingWrapper encryptionOptions(@NotNull EncryptionOptions encryption) {
        hmacSetting.encryptionOptions(encryption);
        return this;
    }

    /**
     * @see HmacSetting
     */
    @AssumeNotNull
    public EncryptionOptionsWrapper<HmacSettingWrapper> encryptionOptions() {
        return new EncryptionOptionsWrapper<>(this);
    }

    /**
     * @see HmacSetting#applicationPolicy(HmacApplicationPolicy)
     */
    @AssumeNotNull
    public HmacSettingWrapper applicationPolicy(@NotNull HmacApplicationPolicy applicationPolicy) {
        hmacSetting.applicationPolicy(applicationPolicy);
        return this;
    }

    /**
     * Sets the hmac setting for the builder and returns it.
     *
     * @return the builder which was used before.
     */
    @AssumeNotNull
    public EncryptionSettingWrapper build() {
        builder.hmac(hmacSetting);
        return builder;
    }

}
