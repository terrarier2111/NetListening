package de.terrarier.netlistening.api.encryption;

import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 * @param <T>
 */
public abstract class EncryptionOptionsSuperBuilder<T> {

    @NotNull
    protected abstract T encryptionOptions(@NotNull EncryptionOptions encryption);

}
