package de.terrarier.netlistening.api.type;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.internals.AssumeNotNull;
import io.netty.buffer.ByteBuf;

/**
 * @author Terrarier2111
 * @since 1.0
 */
public final class DataTypeChar extends DataType<Character> {

    DataTypeChar() {
        super((byte) 0x4, (byte) 2, true);
    }

    @Override
    protected Character read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                             @AssumeNotNull ByteBuf buffer) {
        return buffer.readChar();
    }

    @Override
    protected void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer,
                         @AssumeNotNull Character data) {
        buffer.writeChar(data);
    }

}
