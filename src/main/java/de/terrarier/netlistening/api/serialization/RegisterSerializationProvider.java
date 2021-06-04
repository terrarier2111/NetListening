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
package de.terrarier.netlistening.api.serialization;

import de.terrarier.netlistening.internals.AssumeNotNull;
import de.terrarier.netlistening.utils.ByteBufUtilExtension;
import de.terrarier.netlistening.utils.TypeParameterResolver;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Terrarier2111
 * @since 1.10
 */
@ApiStatus.Experimental
public class RegisterSerializationProvider extends SerializationProvider {

    private static final int HEADER_ID = 0x2766D2D0;
    private final Map<Class<?>, Transformer<?>> classTransformerMapping = new HashMap<>();
    private final Map<Integer, Transformer<?>> idTransformerMapping = new HashMap<>();
    private int transformerId;

    // TODO: Make this class multi thread safe!
    // TODO: Document this class!

    /**
     * @see SerializationProvider#isSerializable(Object)
     */
    @Override
    protected final boolean isSerializable(@AssumeNotNull Object obj) {
        return classTransformerMapping.containsKey(obj.getClass());
    }

    /**
     * @see SerializationProvider#isDeserializable(ReadableByteAccumulation)
     */
    @Override
    protected final boolean isDeserializable(@AssumeNotNull ReadableByteAccumulation ba) {
        final ByteBuf buffer = ba.getBuffer();
        return buffer.readableBytes() > 8 && buffer.getInt(buffer.readerIndex()) == HEADER_ID;
    }

    /**
     * @see SerializationProvider#serialize(WritableByteAccumulation, Object)
     */
    @Override
    protected final void serialize(@AssumeNotNull WritableByteAccumulation ba, @AssumeNotNull Object obj)
            throws Exception {
        final Transformer<?> transformer = classTransformerMapping.get(obj.getClass());
        if (transformer == null) {
            throw new NullPointerException("You tried to use a transformer which was removed which caused a race condition.");
        }
        ensureWritable(ba.getBuffer(), 8);
        System.out.println("writing!");
        ba.getBuffer().writeInt(HEADER_ID);
        ba.getBuffer().writeInt(transformer.id);
        transformer.toBytesUnchecked(ba, obj);
        System.out.println("wrote!");
    }

    /**
     * @see SerializationProvider#deserialize(ReadableByteAccumulation)
     */
    @Override
    protected final Object deserialize(@AssumeNotNull ReadableByteAccumulation ba) throws Exception {
        ba.getBuffer().skipBytes(4);
        final Transformer<?> transformer = idTransformerMapping.get(ba.getBuffer().readInt());
        if (transformer == null) {
            throw new NullPointerException("You tried to use a transformer which was removed which caused a race condition.");
        }
        return transformer.fromBytes0(ba);
    }

    public final int registerTransformer(@AssumeNotNull Transformer<?> transformer) {
        final Class<?> target = TypeParameterResolver.find(transformer, Transformer.class, "T");
        final int transformerId = this.transformerId++;
        transformer.id = transformerId;
        transformer.target = target;
        classTransformerMapping.put(target, transformer);
        idTransformerMapping.put(transformerId, transformer);
        return transformerId;
    }

    public final void unregisterTransformer(int id) {
        final Transformer<?> transformer = idTransformerMapping.remove(id);
        if (transformer == null) {
            throw new IllegalStateException("This transformer is not registered!");
        }
        classTransformerMapping.remove(transformer.target);
    }

    private static abstract class Transformer<T> {

        Class<?> target;
        int id;

        // TODO: Fallback if not transformable!
        protected abstract boolean isTransformableFromBytes0(@AssumeNotNull ReadableByteAccumulation input);

        protected abstract boolean isTransformableToBytes(@AssumeNotNull T obj);

        @AssumeNotNull
        protected abstract T fromBytes0(@AssumeNotNull ReadableByteAccumulation ba)
                throws Exception;

        @SuppressWarnings("unchecked")
        private void toBytesUnchecked(@AssumeNotNull WritableByteAccumulation ba, @AssumeNotNull Object input) throws Exception {
            toBytes0(ba, (T) input);
        }

        protected abstract void toBytes0(@AssumeNotNull WritableByteAccumulation ba, @AssumeNotNull T input) throws Exception;

    }

    public static abstract class ByteArrayTransformer<T> extends Transformer<T> {

        protected final boolean isTransformableFromBytes0(@AssumeNotNull ReadableByteAccumulation input) {
            return isTransformableFromBytes(input.getArray());
        }

        protected boolean isTransformableFromBytes(@AssumeNotNull byte[] input) {
            return true;
        }

        @AssumeNotNull
        protected final T fromBytes0(@AssumeNotNull ReadableByteAccumulation ba) throws Exception {
            final int newBytes = ba.getArray().length - 8;
            final byte[] pass = new byte[newBytes];
            System.arraycopy(ba.getArray(), 8, pass, 0, newBytes);
            return fromBytes(pass);
        }

        @AssumeNotNull
        protected abstract T fromBytes(@AssumeNotNull byte[] data) throws Exception;

        @Override
        protected boolean isTransformableToBytes(@AssumeNotNull T obj) {
            return true;
        }

        @Override
        protected final void toBytes0(@AssumeNotNull WritableByteAccumulation ba, @AssumeNotNull T input)
                throws Exception {
            ba.setArray(toBytes(input));
        }

        @AssumeNotNull
        protected abstract byte[] toBytes(@AssumeNotNull T input) throws Exception;

    }

    public static abstract class ByteBufTransformer<T> extends Transformer<T> {

        @Override
        protected final boolean isTransformableFromBytes0(@AssumeNotNull ReadableByteAccumulation input) {
            return isTransformableFromBytes(input.getBuffer(), input.getLength() - 8);
        }

        protected boolean isTransformableFromBytes(@AssumeNotNull ByteBuf buffer, int length) {
            return true;
        }

        @Override
        protected final T fromBytes0(@AssumeNotNull ReadableByteAccumulation ba) throws Exception {
            return fromBytes(ba.getBuffer(), ba.getLength() - 8);
        }

        @AssumeNotNull
        protected abstract T fromBytes(@AssumeNotNull ByteBuf data, int length) throws Exception;

        @Override
        protected boolean isTransformableToBytes(@AssumeNotNull T obj) {
            return true;
        }

        @Override
        protected final void toBytes0(@AssumeNotNull WritableByteAccumulation ba, @AssumeNotNull T input)
                throws Exception {
            toBytes(ba.getBuffer(), input);
        }

        protected abstract void toBytes(@AssumeNotNull ByteBuf buffer, @AssumeNotNull T input)
                throws Exception;

        @AssumeNotNull
        protected static byte[] readBytes(@AssumeNotNull ByteBuf buffer, int length) {
            return ByteBufUtilExtension.readBytes(buffer, length);
        }

        protected static void writeBytes(@AssumeNotNull ByteBuf buffer, @AssumeNotNull byte[] data) {
            ensureWritable(buffer, data.length);
            buffer.writeBytes(data);
        }

        protected static void ensureWritable(@AssumeNotNull ByteBuf buffer, int bytes) {
            ByteBufUtilExtension.correctSize(buffer, bytes, 0);
        }

    }

}
