package de.terrarier.netlistening.utils;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * @since 1.0
 * @author Terrarier2111
 */
public final class VarIntUtil {

	// Source: https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/util/VarInt.java

	private static final VarIntParseException ONE_BYTE_PARSE_EXCEPTION = new VarIntParseException((byte) 1);
	public static final VarIntParseException FOUR_BYTES_PARSE_EXCEPTION = new VarIntParseException((byte) 4);
	
	private VarIntUtil() {}
	
	public static int varIntSize(int i) {
	    int result = 0;
	    do {
	      result++;
	      i >>>= 7;
	    } while (i != 0);
	    return result;
	  }

	public static void putVarInt(int v, byte[] sink, int offset) {
		do {
			// Encode next 7 bits + terminator bit
			int bits = v & 0x7F;
			v >>>= 7;
			byte b = (byte) (bits + ((v != 0) ? 0x80 : 0));
			sink[offset++] = b;
		} while (v != 0);
	}
	
	public static void putVarInt(int v, byte[] sink) {
		putVarInt(v, sink, 0);
	}
	
	public static byte[] toVarInt(int value) {
		byte[] bytes = new byte[varIntSize(value)];
		putVarInt(value, bytes);
		return bytes;
	}
	
	public static void putVarInt(int v, @NotNull ByteBuf sink) {
		while (true) {
			int bits = v & 0x7f;
			v >>>= 7;
			if (v == 0) {
				sink.writeByte((byte) bits);
				return;
			}
			sink.writeByte((byte) (bits | 0x80));
		}
	}
	
	public static int getVarInt(@NotNull ByteBuf src) throws VarIntParseException {
		if(!src.isReadable())
	    	throw ONE_BYTE_PARSE_EXCEPTION;

	    int tmp;
	    
	    if ((tmp = src.readByte()) >= 0) {
	      return tmp;
	    }
	    if(!src.isReadable()) {
	    	 src.readerIndex(src.readerIndex() - 1);
	    	throw new VarIntParseException((byte) 2);
	    }
	    int result = tmp & 0x7f;
	    
	    if ((tmp = src.readByte()) >= 0) {
	      result |= tmp << 7;
	    } else {
	    	if(!src.isReadable()) {
	    		src.readerIndex(src.readerIndex() - 2);
		    	throw new VarIntParseException((byte) 3);
	    	}
	      result |= (tmp & 0x7f) << 7;
	      if ((tmp = src.readByte()) >= 0) {
	        result |= tmp << 14;
	      } else {
	    	  if(!src.isReadable()) {
	    		  src.readerIndex(src.readerIndex() - 3);
			    	throw FOUR_BYTES_PARSE_EXCEPTION;
	    	  }
	        result |= (tmp & 0x7f) << 14;
	        if ((tmp = src.readByte()) >= 0) {
	          result |= tmp << 21;
	        } else {
	        	if(!src.isReadable()) {
	        		src.readerIndex(src.readerIndex() - 4);
			    	throw new VarIntParseException((byte) 5);
	        	}
	          result |= (tmp & 0x7f) << 21;
	          result |= (tmp = src.readByte()) << 28;
	          while (tmp < 0) {
	            // We get into this loop only in the case of overflow.
	            // By doing this, we can call getVarInt() instead of
	            // getVarLong() when we only need an int.
	            tmp = src.readByte();
	          }
	        }
	      }
	    }
	    return result;
	  }
	
	public static class VarIntParseException extends Exception {

		public final byte requiredBytes;
		
		public VarIntParseException(byte requiredBytes) {
			this.requiredBytes = requiredBytes;
		}

		@NotNull
		@Override
		public Throwable initCause(@NotNull Throwable cause) {
			return this;
		}

		@NotNull
		@Override
		public Throwable fillInStackTrace() {
			return this;
		}
		
	}

}
