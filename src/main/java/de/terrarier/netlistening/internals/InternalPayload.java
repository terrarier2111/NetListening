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
package de.terrarier.netlistening.internals;

import de.terrarier.netlistening.impl.ApplicationImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@ApiStatus.Internal
public abstract class InternalPayload {

    private static final InternalPayloadRegisterPacket REGISTER_PACKET = new InternalPayloadRegisterPacket(-1);
    public static final InternalPayloadHandshake HANDSHAKE = new InternalPayloadHandshake();
    static final InternalPayloadEncryptionInit ENCRYPTION_INIT = new InternalPayloadEncryptionInit();
    static final InternalPayloadEncryptionFinish ENCRYPTION_FINISH = new InternalPayloadEncryptionFinish();
    public static final InternalPayloadPushRequest PUSH_REQUEST = new InternalPayloadPushRequest();
    private static final InternalPayloadUpdateTranslationEntry UPDATE_TRANSLATION_ENTRY = new InternalPayloadUpdateTranslationEntry(-1);

    protected final byte id;

    InternalPayload(byte id) {
        this.id = id;
    }

    final void write0(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer) {
        checkWriteable(application, buffer, 1);
        buffer.writeByte(id);
        write(application, buffer);
    }

    abstract void write(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer);

    abstract void read(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ConnectionImpl connection,
                       @AssumeNotNull ByteBuf buffer) throws CancelReadSignal;

    @AssumeNotNull
    static InternalPayload fromId(byte id) {
        switch (id) {
            case 0x1:
                return REGISTER_PACKET;
            case 0x2:
                return HANDSHAKE;
            case 0x3:
                return ENCRYPTION_INIT;
            case 0x4:
                return ENCRYPTION_FINISH;
            case 0x5:
                return PUSH_REQUEST;
            case 0x6:
                return UPDATE_TRANSLATION_ENTRY;
            default:
                // TODO: Call invalid data event here!
                throw new IllegalStateException("Tried to process an internal payload with an invalid id! (" +
                        Integer.toHexString(id) + ')');
        }
    }

    static void checkReadable(@AssumeNotNull ByteBuf buffer, int additional) throws CancelReadSignal {
        if (buffer.readableBytes() < additional) {
            throw new CancelReadSignal(additional);
        }
    }

    static void checkWriteable(@AssumeNotNull ApplicationImpl application, @AssumeNotNull ByteBuf buffer, int length) {
        ByteBufUtilExtension.correctSize(buffer, length, application.getBuffer());
    }

}
