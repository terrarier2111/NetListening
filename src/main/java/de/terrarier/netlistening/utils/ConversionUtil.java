package de.terrarier.netlistening.utils;

import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class ConversionUtil {
	
	private ConversionUtil() {}
	
	public static byte[] serialize(@NotNull Object obj) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			final ObjectOutputStream os = new ObjectOutputStream(out);
			os.writeObject(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return out.toByteArray();
	}

	public static Object deserialize(byte[] data) {
		final ByteArrayInputStream in = new ByteArrayInputStream(data);
		try {
			final ObjectInputStream is = new ObjectInputStream(in);
			return is.readObject();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

}
