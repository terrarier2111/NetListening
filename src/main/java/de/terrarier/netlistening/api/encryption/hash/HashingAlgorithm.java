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
package de.terrarier.netlistening.api.encryption.hash;

import de.terrarier.netlistening.internal.AssumeNotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public enum HashingAlgorithm {

    SHA_2, SHA_3, SHA_256;

    private static final HashingAlgorithm[] VALUES = values();
    private final String rawName = name().replace('_', '-');
    private final String macName = "Hmac" + name().replaceFirst("_", "");

    /**
     * @return the name of the hashing algorithm in a raw form.
     */
    @AssumeNotNull
    public String getRawName() {
        return rawName;
    }

    /**
     * @return the name of the hashing algorithm in form which can be
     * used to initialize a secret key.
     */
    @AssumeNotNull
    public String getMacName() {
        return macName;
    }

    /**
     * Maps an ordinal number to its respective algorithm.
     *
     * @param id the ordinal of the algorithm which should be returned.
     * @return the algorithm which has an ordinal of id.
     */
    @AssumeNotNull
    public static HashingAlgorithm fromId(byte id) {
        return VALUES[id];
    }

}
