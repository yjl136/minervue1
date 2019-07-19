package android_serialport_api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Locale;

import org.apache.commons.net.util.Base64;

/**
 * Utilities for encoding and decoding binary data to and from different forms.
 */
public class BinaryUtils {

	/** Default encoding when extracting binary data from a String */
	private static final String DEFAULT_ENCODING = "UTF-8";

	/**
	 * Converts byte data to a Hex-encoded string.
	 * 
	 * @param data
	 *            data to hex encode.
	 * 
	 * @return hex-encoded string.
	 */
	public static String toHex(byte[] data, int length) {
		StringBuilder sb = new StringBuilder(length * 2);
		for (int i = 0; i < length; i++) {
			String hex = Integer.toHexString(data[i]);
			if (hex.length() == 1) {
				// Append leading zero.
				sb.append("0");
			} else if (hex.length() == 8) {
				// Remove ff prefix from negative numbers.
				hex = hex.substring(6);
			}
			sb.append(hex);
			sb.append(" ");
		}
		return sb.toString().toLowerCase(Locale.getDefault());
	}

	/**
	 * Converts a Hex-encoded data string to the original byte data.
	 * 
	 * @param hexData
	 *            hex-encoded data to decode.
	 * @return decoded data from the hex string.
	 */
	public static byte[] fromHex(String hexData) {
		byte[] result = new byte[(hexData.length() + 1) / 2];
		String hexNumber = null;
		int stringOffset = 0;
		int byteOffset = 0;
		while (stringOffset < hexData.length()) {
			hexNumber = hexData.substring(stringOffset, stringOffset + 2);
			stringOffset += 2;
			result[byteOffset++] = (byte) Integer.parseInt(hexNumber, 16);
		}
		return result;
	}

	/**
	 * Converts byte data to a Base64-encoded string.
	 * 
	 * @param data
	 *            data to Base64 encode.
	 * @return encoded Base64 string.
	 */
	public static String toBase64(byte[] data) {
		byte[] b64 = Base64.encodeBase64(data);
		return new String(b64);
	}

	/**
	 * Converts a Base64-encoded string to the original byte data.
	 * 
	 * @param b64Data
	 *            a Base64-encoded string to decode.
	 * 
	 * @return bytes decoded from a Base64 string.
	 */
	public static byte[] fromBase64(String b64Data) {
		byte[] decoded;
		try {
			decoded = Base64.decodeBase64(b64Data.getBytes(DEFAULT_ENCODING));
		} catch (UnsupportedEncodingException uee) {
			// Shouldn't happen if the string is truly Base64 encoded.
			decoded = Base64.decodeBase64(b64Data.getBytes());
		}
		return decoded;
	}

	/**
	 * Wraps a ByteBuffer in an InputStream.
	 * 
	 * @param byteBuffer
	 *            The ByteBuffer to wrap.
	 * 
	 * @return An InputStream wrapping the ByteBuffer content.
	 */
	public static InputStream toStream(ByteBuffer byteBuffer) {
		byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		return new ByteArrayInputStream(bytes);
	}

	public static byte[] getBytes(InputStream is) throws IOException {
		int len;
		int size = 1024;
		byte[] buf;

		if (is instanceof ByteArrayInputStream) {
			size = is.available();
			buf = new byte[size];
			len = is.read(buf, 0, size);
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];
			while ((len = is.read(buf, 0, size)) != -1)
				bos.write(buf, 0, len);
			buf = bos.toByteArray();
		}
		return buf;
	}
	public static int isOdd(int paramInt)
	  {
	    return paramInt & 0x1;
	  }
}
