package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.api.encryption.CipherEncryptionAlgorithm;
import de.terrarier.netlistening.api.encryption.EncryptionOptions;
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
    @NotNull
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
    @NotNull
    public HmacSetting encryptionOptions(@NotNull EncryptionOptions encryption) {
        if(encryptionOptions != null) { // TODO: Check why this if statement is here!
            encryption.getKeySize(); // This line makes sure that checkBuilt was called before any get ops could be performed.
            this.encryptionOptions = encryption;
        }
        return this;
    }

    /**
     * Sets in which case a hmac is to be sent besides the traffic.
     *
     * @param useCase the useCase in which a hmac is to be sent besides the traffic.
     * @return the local reference.
     */
    @NotNull
    public HmacSetting useCase(@NotNull HmacUseCase useCase) {
        this.useCase = useCase;
        return this;
    }

    /**
     * @return the hashing algorithm set by the user or otherwise by default.
     */
    @NotNull
    public HashingAlgorithm getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    /**
     * @return the encryption configuration set by the user or otherwise by default.
     */
    @NotNull
    public EncryptionOptions getEncryptionSetting() {
        return encryptionOptions;
    }

    /**
     * @return the useCase in which a hmac is sent besides the traffic.
     */
    @NotNull
    public HmacUseCase getUseCase() {
        return useCase;
    }

}
