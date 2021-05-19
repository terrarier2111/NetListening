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
package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public enum CipherAlgorithmMode {

    NONE, CBC, CCM, CFB, CFBx, CTR, CTS, ECB, GCM, OFB, OFBx, PCBC;

    private static final CipherAlgorithmMode[] VALUES = values();

    /**
     * Maps an ordinal number to its respective mode.
     *
     * @param id the ordinal of the mode which should be returned.
     * @return the mode which has an ordinal of id.
     */
    @AssumeNotNull
    public static CipherAlgorithmMode fromId(byte id) {
        return VALUES[id];
    }

}
