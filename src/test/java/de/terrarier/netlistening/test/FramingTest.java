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
package de.terrarier.netlistening.test;

import de.terrarier.netlistening.Client;
import de.terrarier.netlistening.Server;
import de.terrarier.netlistening.api.encryption.EncryptionOptions;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionData;
import de.terrarier.netlistening.api.encryption.SymmetricEncryptionUtil;
import de.terrarier.netlistening.api.event.ConnectionPostInitEvent;
import de.terrarier.netlistening.api.event.ConnectionPostInitListener;
import de.terrarier.netlistening.api.event.DecodeEvent;
import de.terrarier.netlistening.api.event.DecodeListener;
import de.terrarier.netlistening.api.type.DataType;
import de.terrarier.netlistening.impl.ClientImpl;
import de.terrarier.netlistening.impl.ConnectionImpl;
import de.terrarier.netlistening.impl.ServerImpl;
import de.terrarier.netlistening.util.ByteBufUtilExtension;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static de.terrarier.netlistening.util.ByteBufUtilExtension.getBytesAndRelease;

public final class FramingTest {

    private static final byte[] KEY = {21, -18, 41, -103, 16, 42, -104, 30, -15, -113, 75, -122, 59, -13, -104, 109}; // it is okay to leak this key as it is test-only!

    @Test
    public void testEncryptionFraming() {
        final ServerImpl server = (ServerImpl) Server.builder(55843).compression().varIntCompression(false).nibbleCompression(false)
                .build().encryption().disableHmac().build().build();
        server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                final byte[] data = value.getData().read();
                System.out.println("Received data: " + Arrays.toString(data));
            }
        });
       /* server.registerListener(new ConnectionPostInitListener() { // used to get a key as a nice-lookin' byte array!
            @Override
            public void trigger(ConnectionPostInitEvent value) {
                final byte[] key = ((ConnectionImpl) value.getConnection()).getEncryptionContext().getSecretKey().getEncoded();
                final StringBuilder sb = new StringBuilder(2 + key.length * 3 - 2 + 2);
                sb.append("{ ");
                for(byte part : key) {
                    if(sb.length() != 2) {
                        sb.append(", ");
                    }
                    // sb.append("0x").append(Integer.toHexString(part & 0xFF));
                    sb.append(part);
                }
                sb.insert(sb.length(), " }");
                System.out.println("Key: " + sb);
            }
        });*/
        server.registerListener(new ConnectionPostInitListener() {
            @Override
            public void trigger(ConnectionPostInitEvent value) {
                ((ConnectionImpl) value.getConnection()).setSymmetricKey(server.getEncryptionSetting().getSymmetricSetting(), KEY);
            }
        });
        final Client client = Client.builder("localhost", 55843).build();
        try {
            Thread.sleep(250L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            // send the register
            ByteBuf registerBuffer = Unpooled.buffer();
            registerBuffer.writeInt(0x0);
            registerBuffer.writeByte(0x1);
            registerBuffer.writeInt(0x5); // packet id - starting at 0x5
            registerBuffer.writeShort(0x1); // 1 data type
            registerBuffer.writeByte(DataType.BYTE_ARRAY.getId() - 1);
            final Field field = ClientImpl.class.getDeclaredField("channel");
            field.setAccessible(true);
            Thread.sleep(200L);
            final Channel channel = (Channel) field.get(client);
            channel.writeAndFlush(registerBuffer);
            Thread.sleep(200L);

            // prepare the fake packet to send
            final ByteBuf fakePacket = Unpooled.buffer();
            fakePacket.writeInt(0x5); // Registered packet
            fakePacket.writeInt(0x2); // Byte array length
            fakePacket.writeByte(0xF); // Just a random number
            fakePacket.writeByte(0x3); // Just a random number

            // send the payload
            final SecretKey secretKey = SymmetricEncryptionUtil.readSecretKey(KEY,
                    server.getEncryptionSetting().getSymmetricSetting());
            final Constructor<SymmetricEncryptionData> constructor = SymmetricEncryptionData.class.getDeclaredConstructor(EncryptionOptions.class, SecretKey.class);
            constructor.setAccessible(true);
            final SymmetricEncryptionData symmetricEncryptionData = constructor.newInstance(server.getEncryptionSetting().getSymmetricSetting(), secretKey);

            ByteBuf data = Unpooled.buffer();
            data.writeInt(0x3);
            Method method = SymmetricEncryptionUtil.class.getDeclaredMethod("encrypt", byte[].class, SymmetricEncryptionData.class);
            method.setAccessible(true);
            byte[] encryptedData = (byte[]) method.invoke(null,
                    getBytesAndRelease(fakePacket), symmetricEncryptionData); // get all bytes from data except the last one
            final int size = encryptedData.length;
            ByteBufUtilExtension.correctSize(data, 4 + size, 0);
            data.writeInt(size);
            data.writeBytes(encryptedData, 0, encryptedData.length - 1);
            channel.writeAndFlush(data);
            Thread.sleep(200L);
            data = Unpooled.buffer();
            data.writeByte(encryptedData[encryptedData.length - 1]);
            channel.writeAndFlush(data);
            Thread.sleep(200L);
        } catch (NoSuchFieldException | IllegalAccessException | InterruptedException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        server.stop();
        client.stop();
    }

    @Test
    public void testEncryptionFraming2() {
        final ServerImpl server = (ServerImpl) Server.builder(55844).compression().varIntCompression(false).nibbleCompression(false)
                .build().encryption().disableHmac().build().build();
        server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                final byte[] data = value.getData().read();
                System.out.println("Received data: " + Arrays.toString(data));
            }
        });
       /* server.registerListener(new ConnectionPostInitListener() { // used to get a key as a nice-lookin' byte array!
            @Override
            public void trigger(ConnectionPostInitEvent value) {
                final byte[] key = ((ConnectionImpl) value.getConnection()).getEncryptionContext().getSecretKey().getEncoded();
                final StringBuilder sb = new StringBuilder(2 + key.length * 3 - 2 + 2);
                sb.append("{ ");
                for(byte part : key) {
                    if(sb.length() != 2) {
                        sb.append(", ");
                    }
                    // sb.append("0x").append(Integer.toHexString(part & 0xFF));
                    sb.append(part);
                }
                sb.insert(sb.length(), " }");
                System.out.println("Key: " + sb);
            }
        });*/
        server.registerListener(new ConnectionPostInitListener() {
            @Override
            public void trigger(ConnectionPostInitEvent value) {
                ((ConnectionImpl) value.getConnection()).setSymmetricKey(server.getEncryptionSetting().getSymmetricSetting(), KEY);
            }
        });
        final Client client = Client.builder("localhost", 55844).build();
        try {
            Thread.sleep(250L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            // send the register
            ByteBuf registerBuffer = Unpooled.buffer();
            registerBuffer.writeInt(0x0);
            registerBuffer.writeByte(0x1);
            registerBuffer.writeInt(0x5); // packet id - starting at 0x5
            registerBuffer.writeShort(0x1); // 1 data type
            registerBuffer.writeByte(DataType.BYTE_ARRAY.getId() - 1);
            final Field field = ClientImpl.class.getDeclaredField("channel");
            field.setAccessible(true);
            Thread.sleep(200L);
            final Channel channel = (Channel) field.get(client);
            channel.writeAndFlush(registerBuffer);
            Thread.sleep(200L);

            // prepare the fake packet to send
            final ByteBuf fakePacket = Unpooled.buffer();
            fakePacket.writeInt(0x5); // Registered packet
            fakePacket.writeInt(0x99); // Too large byte array length

            // send the payload
            final SecretKey secretKey = SymmetricEncryptionUtil.readSecretKey(KEY,
                    server.getEncryptionSetting().getSymmetricSetting());
            final Constructor<SymmetricEncryptionData> constructor = SymmetricEncryptionData.class.getDeclaredConstructor(EncryptionOptions.class, SecretKey.class);
            constructor.setAccessible(true);
            final SymmetricEncryptionData symmetricEncryptionData = constructor.newInstance(server.getEncryptionSetting().getSymmetricSetting(), secretKey);

            ByteBuf data = Unpooled.buffer();
            data.writeInt(0x3);
            Method method = SymmetricEncryptionUtil.class.getDeclaredMethod("encrypt", byte[].class, SymmetricEncryptionData.class);
            method.setAccessible(true);
            byte[] encryptedData = (byte[]) method.invoke(null,
                    getBytesAndRelease(fakePacket), symmetricEncryptionData); // get all bytes from data except the last one
            final int size = encryptedData.length;
            ByteBufUtilExtension.correctSize(data, 4 + size, 0);
            data.writeInt(size);
            data.writeBytes(encryptedData, 0, encryptedData.length - 1);
            channel.writeAndFlush(data);
            Thread.sleep(200L);
            data = Unpooled.buffer();
            data.writeByte(encryptedData[encryptedData.length - 1]);
            channel.writeAndFlush(data);
            Thread.sleep(200L);
        } catch (NoSuchFieldException | IllegalAccessException | InterruptedException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
        server.stop();
        client.stop();
    }

    @Test
    public void testKeepAliveFraming() {
        final ServerImpl server = (ServerImpl) Server.builder(55844).compression().varIntCompression(false).nibbleCompression(false)
                .build().encryption().disableHmac().build().build();
        server.registerListener(new DecodeListener() {
            @Override
            public void trigger(DecodeEvent value) {
                final byte[] data = value.getData().read();
                System.out.println("Received data: " + Arrays.toString(data));
            }
        });
        final Client client = Client.builder("localhost", 55844).build();
        try {
            Thread.sleep(250L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            ByteBuf registerBuffer = Unpooled.buffer();
            registerBuffer.writeInt(0x0);
            registerBuffer.writeByte(0x1);
            registerBuffer.writeInt(0x5); // packet id - starting at 0x5
            registerBuffer.writeShort(0x1); // 1 data type
            registerBuffer.writeByte(DataType.BYTE_ARRAY.getId() - 1);
            final Field field = ClientImpl.class.getDeclaredField("channel");
            field.setAccessible(true);
            Thread.sleep(200L);
            final Channel channel = (Channel) field.get(client);
            channel.writeAndFlush(registerBuffer);
            Thread.sleep(200L);
            ByteBuf payload = Unpooled.buffer();
            payload.writeInt(0x1);
            channel.writeAndFlush(payload);
            Thread.sleep(200L);
            payload = Unpooled.buffer();
            payload.writeByte(Byte.MIN_VALUE + 1);
            payload.writeInt(0x5); // Registered packet
            payload.writeInt(0x2); // Byte array length
            payload.writeByte(0xF); // Just a random number
            payload.writeByte(0x3); // Just a random number
            channel.writeAndFlush(payload);
            Thread.sleep(200L);
        } catch (InterruptedException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
