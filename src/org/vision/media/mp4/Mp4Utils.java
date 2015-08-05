/***
 * The content of this file or document is CONFIDENTIAL and PROPRIETARY
 * to ChengZhen(anyou@msn.com). It is subject to the terms of a
 * License Agreement between Licensee and ChengZhen(anyou@msn.com).
 * restricting among other things, the use, reproduction, distribution
 * and transfer. Each of the embodiments, including this information and
 * any derivative work shall retain this copyright notice.
 *
 * Copyright (c) 2005-2014 ChengZhen(anyou@msn.com) All Rights Reserved.
 *
 */
package org.vision.media.mp4;

import java.nio.ByteBuffer;

public class Mp4Utils {

	/**
	 * Turns a hex encoded string into a byte array. It is specifically meant to
	 * "reverse" the toHex(byte[]) method.
	 * 
	 * @param hex
	 *            a hex encoded String to transform into a byte array.
	 * @return a byte array representing the hex String[
	 */
	public static byte[] decodeHex(String hex) {
		char[] chars = hex.toCharArray();
		byte[] bytes = new byte[chars.length / 2];
		int byteCount = 0;
		for (int i = 0; i < chars.length; i += 2) {
			int newByte = 0x00;
			newByte |= hexCharToByte(chars[i]);
			newByte <<= 4;
			newByte |= hexCharToByte(chars[i + 1]);
			bytes[byteCount] = (byte) newByte;
			byteCount++;
		}
		return bytes;
	}
	
	/**
	 * Turns an array of bytes into a String representing each byte as an
	 * unsigned hex number.
	 * <p/>
	 * Method by Santeri Paavolainen, Helsinki Finland 1996<br>
	 * (c) Santeri Paavolainen, Helsinki Finland 1996<br>
	 * Distributed under LGPL.
	 * 
	 * @param bytes
	 *            an array of bytes to convert to a hex-string
	 * @return generated hex string
	 */
	public static String encodeHex(byte[] bytes) {
		StringBuilder buf = new StringBuilder(bytes.length * 2);
		int i;

		for (i = 0; i < bytes.length; i++) {
			if ((bytes[i] & 0xff) < 0x10) {
				buf.append("0");
			}
			buf.append(Long.toString(bytes[i] & 0xff, 16));
		}
		return buf.toString();
	}

	/**
	 * Returns the the byte value of a hexadecmical char (0-f). It's assumed
	 * that the hexidecimal chars are lower case as appropriate.
	 * @param ch a hexedicmal character (0-f)
	 * @return the byte value of the character (0x00-0x0F)
	 */
	public static byte hexCharToByte(char ch) {
		switch (ch) {
		case '0':
			return 0x00;
		case '1':
			return 0x01;
		case '2':
			return 0x02;
		case '3':
			return 0x03;
		case '4':
			return 0x04;
		case '5':
			return 0x05;
		case '6':
			return 0x06;
		case '7':
			return 0x07;
		case '8':
			return 0x08;
		case '9':
			return 0x09;
		case 'a':
			return 0x0A;
		case 'b':
			return 0x0B;
		case 'c':
			return 0x0C;
		case 'd':
			return 0x0D;
		case 'e':
			return 0x0E;
		case 'f':
			return 0x0F;
		}
		return 0x00;
	}

	/**
	 * 指出指定的字符器是否为空或只有空白字符
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isBlank(String str) {
		int strLen;
		if ((str == null) || ((strLen = str.length()) == 0)) {
			return true;
		}

		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	/** 指出指定的字符器是否为空. */
	public static boolean isEmpty(String str) {
		return (str == null) || (str.length() == 0);
	}

	/**
	 * 指出指定的字符器是否不为空或有非空白字符
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNotBlank(String str) {
		int strLen;
		if ((str == null) || ((strLen = str.length()) == 0)) {
			return false;
		}

		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}

		return false;
	}

	public static int parseInt(Object value) {
		return parseInt(value, -1);
	}

	public static int parseInt(Object value, int defaultValue) {
		try {
			if (value == null) {
				return defaultValue;

			} else if (value instanceof Number) {
				return ((Number) value).intValue();

			} else if (value instanceof Boolean) {
				return ((Boolean) value) ? 1 : 0;

			} else {
				return Integer.parseInt(value.toString());
			}

		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int parseInt(String text) {
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return -1;
		}
	}

	public static long parseLong(Object text) {
		try {
			if (text == null) {
				return 0;
			} else if (text instanceof Number) {
				return ((Number) text).longValue();
			} else {
				return Long.parseLong(text.toString());
			}
		} catch (Exception e) {
			return -1;
		}
	}

	public static long parseLong(String text) {
		try {
			return Long.parseLong(text);
		} catch (Exception e) {
			return -1;
		}
	}

	public static Mp4QueryString parseQueryString(String substring) {
		try {
			return new Mp4QueryString(substring);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static int byteIndexOf(ByteBuffer buffer, byte[] temp, int offset) {
		int i;
		if (temp.length == 0) {
			return 0;
		}

		int end = buffer.limit() - temp.length;
		if (end < 0) {
			return -1;
		}

		int start = buffer.position() + offset;
		if (start > end) {
			return -1;
		}

		if (start < 0) {
			start = 0;
		}

		byte[] b = buffer.array();

		Search: for (i = start; i < end; i++) {
			if (b[i] != temp[0]) {
				continue;
			}

			int k = 1;
			while (k < temp.length) {
				if (b[k + i] != temp[k]) {
					continue Search;
				}
				k++;
			}

			return i;
		}

		return -1;
	}

}
