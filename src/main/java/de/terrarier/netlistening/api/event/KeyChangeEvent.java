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

/**
 * This event can be used to detect MITM attacks.
 * It is only called on the client side.
 *
 * @author Terrarier2111
 * @since 1.0
 */
public final class KeyChangeEvent extends Cancellable implements Event {

    private final byte[] currentKeyHash;
    private final byte[] receivedKeyHash;
    private final KeyChangeResult result;

    @ApiStatus.Internal
    public KeyChangeEvent(@AssumeNotNull byte[] currentKeyHash, @AssumeNotNull byte[] receivedKeyHash,
                          @AssumeNotNull KeyChangeResult result) {
        this.currentKeyHash = currentKeyHash;
        this.receivedKeyHash = receivedKeyHash;
        this.result = result;
    }

    /**
     * @return the hash of the key which was previously received from the server.
     */
    @AssumeNotNull
    public byte[] getCurrentKeyHash() {
        return currentKeyHash;
    }

    /**
     * @return the hash of the received key.
     */
    @AssumeNotNull
    public byte[] getReceivedKeyHash() {
        return receivedKeyHash;
    }

    /**
     * @return the result of the key change.
     */
    @AssumeNotNull
    public KeyChangeResult getResult() {
        return result;
    }

    public enum KeyChangeResult {

        HASH_ABSENT, HASH_CHANGED, HASH_EQUAL

    }

}
