package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.encryption.hash.HmacSettingWrapper;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class EncryptionSettingWrapper extends EncryptionOptionsSuperBuilder<EncryptionSettingWrapper> {

    private final EncryptionSetting encryptionSetting = new EncryptionSetting();
    private final Server.Builder builder;

    public EncryptionSettingWrapper(@NotNull Server.Builder builder) {
        this.builder = builder;
    }

    /**
     * @see EncryptionSetting
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
     * @see EncryptionSetting
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
     * @see EncryptionSetting
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
     * @see EncryptionSetting
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
     * @see EncryptionOptionsSuperBuilder
     */
    @AssumeNotNull
    @Override
    protected EncryptionSettingWrapper encryptionOptions(@NotNull EncryptionOptions encryption) {
        asymmetricEncryptionOptions(encryption);
        return this;
    }

}
