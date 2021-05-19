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
import org.jetbrains.annotations.NotNull;

/**
 * @param <T> the type of the underlying builder.
 * @author Terrarier2111
 * @since 1.0
 */
public abstract class EncryptionOptionsSuperBuilder<T extends EncryptionOptionsSuperBuilder<T>> {

    /**
     * Sets the encryption options for the underlying builder to
     * the setting passed as the parameter.
     *
     * @param encryption the encryption options which get set for the underlying builder.
     * @return the underlying builder.
     */
    @AssumeNotNull
    protected abstract T encryptionOptions(@NotNull EncryptionOptions encryption);

}
