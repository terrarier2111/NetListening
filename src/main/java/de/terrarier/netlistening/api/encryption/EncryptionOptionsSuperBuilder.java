package de.terrarier.netlistening.api.encryption;

import de.terrarier.netlistening.internals.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 * @param <T> the type of the underlying builder.
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
