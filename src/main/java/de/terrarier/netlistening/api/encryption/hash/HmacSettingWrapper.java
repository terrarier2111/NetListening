package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.api.encryption.EncryptionOptions;
import de.terrarier.netlistening.api.encryption.EncryptionOptionsSuperBuilder;
import de.terrarier.netlistening.api.encryption.EncryptionOptionsWrapper;
import de.terrarier.netlistening.api.encryption.EncryptionSettingWrapper;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class HmacSettingWrapper extends EncryptionOptionsSuperBuilder<HmacSettingWrapper> {

    private final HmacSetting hmacSetting = new HmacSetting();
    private final EncryptionSettingWrapper builder;

    public HmacSettingWrapper(@NotNull EncryptionSettingWrapper builder) {
        this.builder = builder;
    }

    /**
     * @see HmacSetting
     */
    @AssumeNotNull
    public HmacSettingWrapper hashingAlgorithm(@NotNull HashingAlgorithm hashingAlgorithm) {
        hmacSetting.hashingAlgorithm(hashingAlgorithm);
        return this;
    }

    /**
     * @see HmacSetting
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
     * @see HmacSetting
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
