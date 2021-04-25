package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.api.encryption.CipherEncryptionAlgorithm;
import de.terrarier.netlistening.api.encryption.EncryptionOptions;
import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class HmacSetting {

    private EncryptionOptions encryptionOptions = new EncryptionOptions().type(CipherEncryptionAlgorithm.AES);
    private HashingAlgorithm hashingAlgorithm = HashingAlgorithm.SHA_256;
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
