package de.terrarier.netlistening.utils;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.03
 * @author Terrarier2111
 */
@ApiStatus.Internal
public final class ClosingBuffer implements AutoCloseable {

    private final ByteBuf buffer;

    public ClosingBuffer(ByteBuf buffer) {
        this.buffer = buffer;
    }

    @NotNull
    public ByteBuf getBuffer() {
        return buffer;
    }

    @Override
    public void close() {
        buffer.release();
    }

}
