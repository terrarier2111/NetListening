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
    private HmacUseCase useCase = HmacUseCase.ENCRYPTED;

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
     * Sets in which case a hmac is to be sent besides the traffic.
     *
     * @param useCase the useCase in which a hmac is to be sent besides the traffic.
     * @return the local reference.
     */
    @AssumeNotNull
    public HmacSetting useCase(@NotNull HmacUseCase useCase) {
        this.useCase = useCase;
        return this;
    }

    /**
     * @return the hashing algorithm set by the user or otherwise by default.
     */
    @AssumeNotNull
    public HashingAlgorithm getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    /**
     * @return the encryption configuration set by the user or otherwise by default.
     */
    @AssumeNotNull
    public EncryptionOptions getEncryptionSetting() {
        return encryptionOptions;
    }

    /**
     * @return the useCase in which a hmac is sent besides the traffic.
     */
    @AssumeNotNull
    public HmacUseCase getUseCase() {
        return useCase;
    }

}
