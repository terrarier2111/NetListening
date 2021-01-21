package de.terrarier.netlistening.api.event;

import org.jetbrains.annotations.NotNull;

/**
 * This event can be used to detect MITM attacks.
 * It is only called on the client side.
 *
 * @since 1.0
 * @author Terrarier2111
 */
public final class KeyChangeEvent extends Cancellable implements Event {

    private final byte[] currentKeyHash;
    private final byte[] receivedKeyHash;
    private final KeyChangeResult result;

    public KeyChangeEvent(byte[] currentKeyHash, byte[] receivedKeyHash, @NotNull KeyChangeResult result) {
        this.currentKeyHash = currentKeyHash;
        this.receivedKeyHash = receivedKeyHash;
        this.result = result;
    }

    /**
     * @return the hash of the key which was previously received from the server.
     */
    public byte[] getCurrentKeyHash() {
        return currentKeyHash;
    }

    /**
     * @return the hash of the received key.
     */
    public byte[] getReceivedKeyHash() {
        return receivedKeyHash;
    }

    /**
     * @return the result of the key change.
     */
    @NotNull
    public KeyChangeResult getResult() {
        return result;
    }

    public enum KeyChangeResult {

        HASH_ABSENT, HASH_CHANGED, HASH_EQUAL

    }

}
