package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.encryption.hash.HmacSetting;
import de.terrarier.netlistening.api.encryption.hash.HmacSettingWrapper;
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
    @NotNull
    public EncryptionSettingWrapper asymmetricEncryptionOptions(@NotNull EncryptionOptions asymmetricEncryption) {
        encryptionSetting.asymmetricEncryptionOptions(asymmetricEncryption);
        return this;
    }

    /**
     * @see EncryptionSetting
     */
    @NotNull
    public EncryptionOptionsWrapper<EncryptionSettingWrapper> asymmetricEncryptionOptions() {
        return new EncryptionOptionsWrapper<>(this);
    }

    /**
     * @see EncryptionSetting
     */
    @NotNull
    public EncryptionOptionsWrapper<EncryptionSettingWrapper> symmetricEncryptionOptions(@NotNull EncryptionOptions symmetricEncryption) {
        return new EncryptionOptionsWrapper<>(this, symmetricEncryption);
    }

    /**
     * @see EncryptionSetting
     */
    @NotNull
    public EncryptionOptionsWrapper<EncryptionSettingWrapper> symmetricEncryptionOptions() {
        return new EncryptionOptionsWrapper<>(this);
    }

    /**
     * @see EncryptionSetting
     */
    @NotNull
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
    @NotNull
    public HmacSettingWrapper hmac() {
        return new HmacSettingWrapper(this);
    }

    /**
     * @see EncryptionSetting
     */
    @NotNull
    public EncryptionSettingWrapper disableHmac() {
        return hmac(null);
    }

    /**
     * Sets the encryption setting for the builder
     * and returns it.
     *
     * @return the builder which was used before.
     */
    @NotNull
    public Server.Builder build() {
        return builder.encryption(encryptionSetting);
    }

    /**
     * @see EncryptionOptionsSuperBuilder
     */
    @NotNull
    @Override
    protected EncryptionSettingWrapper encryptionOptions(@NotNull EncryptionOptions encryption) {
        asymmetricEncryptionOptions(encryption);
        return this;
    }

}
