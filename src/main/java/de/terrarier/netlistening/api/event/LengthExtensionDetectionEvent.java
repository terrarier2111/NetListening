package de.terrarier.netlistening.api.event;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class LengthExtensionDetectionEvent implements Event {

    private Result result = Result.DROP_DATA;
    private final byte[] expectedHash;
    private final byte[] actualHash;

    public LengthExtensionDetectionEvent(byte[] expectedHash, byte[] actualHash) {
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
    }

    /**
     * @return the actual hash code calculated from the data.
     */
    public byte[] getActualHash() {
        return actualHash;
    }

    /**
     * @return the hash received from the other connection.
     */
    public byte[] getExpectedHash() {
        return expectedHash;
    }

    /**
     * @return the result of the event which determines what action should be performed.
     */
    @NotNull
    public Result getResult() {
        return result;
    }

    /**
     * Sets the result of the event which determines what action should be performed.
     *
     * @param result the result of the event which determines what action should be performed.
     */
    public void setResult(@NotNull Result result) {
        this.result = result;
    }

    public enum Result {

        DROP_DATA, IGNORE

    }

}
