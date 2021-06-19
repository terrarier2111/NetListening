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
package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This event can be used to handle detected length extension attacks.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class LengthExtensionDetectionEvent implements Event {

    private final byte[] expectedHash;
    private final byte[] actualHash;
    private Result result = Result.DROP_DATA;

    @ApiStatus.Internal
    public LengthExtensionDetectionEvent(@AssumeNotNull byte[] expectedHash, @AssumeNotNull byte[] actualHash) {
        this.expectedHash = expectedHash;
        this.actualHash = actualHash;
    }

    /**
     * @return the hash received from the other connection.
     */
    @AssumeNotNull
    public byte[] getExpectedHash() {
        return expectedHash;
    }

    /**
     * @return the actual hash code calculated from the data.
     */
    @AssumeNotNull
    public byte[] getActualHash() {
        return actualHash;
    }

    /**
     * @return the result of the event which determines what action should be performed.
     */
    @AssumeNotNull
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
