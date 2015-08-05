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

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 
 * 
 * @author ChengZhen(anyou@msn.com)
 */
abstract class Mp4Stream {

	/** 比特缓存区. */
	private long bitsBuffer = 0;

	/** 比特缓存区中有效数据的长度. */
	private int bitsCount = 0;
	
	public Mp4Stream() {
	}
	
	/**
	 * 关闭当前打开的文件
	 */
	public void close() {
		bitsCount  = 0;
		bitsBuffer = 0;
	}

	
	public void flush() {
		
	}
	
	/**
	 * 返回当前打开的文件的长度.
	 * @return
	 */
	public abstract long getFileSize();

	/**
	 * 返回当前文件指针的位置
	 * @return
	 */
	public abstract long getPosition();

	/**
	 * 指出文件是否是打开的
	 * @return
	 */
	public abstract boolean isOpen() ;

	/**
	 * 指出是否是在写文件
	 * @return
	 */
	public boolean isReadOnly() {
		return false;
	}
	
	/**
	 * 读取一个字节
	 */
	public abstract int read() throws IOException ;

	public final int readBits(int size) throws IOException {
		if ((size <= 0) || (size >= 8)) {
			return 0;
		}

		if (bitsCount == 0) {
			// 缓存区为空, 则从文件读取一个字节
			bitsBuffer = readInt(1);
			bitsCount = 8;
		}

		bitsCount -= size;
		if (bitsCount <= 0) {
			bitsCount = 0;
		}

		long ret = bitsBuffer;
		if (bitsCount > 0) {
			ret >>= bitsCount;
		}

		long mask = 0;
		for (long i = 0; i < size; i++) {
			mask |= 0x00000001 << i;
		}

		return (int) (ret & mask);
	}

	/**
	 * 批量读取指定个字节
	 * @param type
	 * @return
	 * @throws IOException
	 */
	public abstract int readBytes(byte[] dst) throws IOException;
	
	public abstract int readBytes(byte[] dst, int offset, int length) throws IOException;

	/**
	 * 读取一个浮点数.
	 * @param size
	 * @return
	 * @throws IOException
	 */
	public final float readFloat(int size) throws IOException {
		if (size == 4) {
			// 32 位定点数
			int iPart = readInt16();
			int fPart = readInt16();
			return iPart + (((float)fPart) / 0x10000);

		} else if (size == 2) {
			// 16 位定点数
			int iPart = read();
			int fPart = read();
			return iPart + (((float)fPart) / 0x100);

		} else {
			throw new IOException("Invalid float size: " + size);
		}
	}

	/**
	 * 读取一个指定的长度的整数
	 * @param size
	 * @return
	 * @throws IOException
	 */
	public final long readInt(int size) throws IOException {
		switch (size) {
		case 1: return read();
		case 2: return readInt16();
		case 3: return readInt24();
		case 4: return readInt32();
		case 8: return readLong();
		default: throw new IOException("Invalid integer size: " + size);
		}
	}

	/**
	 * 读取两个字节
	 * @return
	 * @throws IOException
	 */
	public final int readInt16() throws IOException {
		int i = read();
		int j = read();
		if ((i | j) < 0) {
			throw new EOFException();
		}

		return (i << 8) + (j << 0);
	}

	/**
	 * 读取三个字节
	 * @return
	 * @throws IOException
	 */
	public final int readInt24() throws IOException {
		int j = read();
		int k = read();
		int l = read();
		if ((j | k | l) < 0) {
			throw new EOFException();
		}
		return (j << 16) + (k << 8) + (l << 0);
	}

	/**
	 * 读取四个字节
	 * @return
	 * @throws IOException
	 */
	public final long readInt32() throws IOException {
		long i = read();
		long j = read();
		long k = read();
		long l = read();
		if ((i | j | k | l) < 0) {
			throw new EOFException();
		}
		long ret = (i << 24) + (j << 16) + (k << 8) + (l << 0);
		return ret;
	}

	/**
	 * 读取八个字节
	 * @return
	 * @throws IOException
	 */
	public final long readLong() throws IOException {
		return (readInt32() << 32) + (readInt32() & 4294967295L);
	}

	/**
	 * 定位到指定的位置.
	 * @param position
	 * @throws IOException
	 */
	public abstract void seek(long position) throws IOException;

	/**
	 * 跳过指定个字节
	 * @param size
	 * @throws IOException
	 */
	public abstract void skip(int size) throws IOException;

	public final void startReadBits() {
		bitsBuffer = 0;
		bitsCount = 0;
	}

	/**
	 * 批量写入多个 byte
	 * @param bytes 要写入的多个 byte 的值.
	 * @throws IOException 发生写错误
	 */
	public abstract void write(byte[] bytes) throws IOException;

	public abstract void write(byte[] bytes, int offset, int len) throws IOException;

	/**
	 * 写入一个 byte.
	 * @param value 要写入的 byte 的值.
	 * @throws IOException 发生写错误
	 */
	public abstract void write(int value) throws IOException;

	/**
	 * 
	 * @param value 要写入的整数的值.
	 * @param size
	 * @throws IOException
	 */
	public final int writeBits(long value, int size) throws IOException {
		if ((size <= 0) || (size >= 8)) {
			return 0;
		}

		if (bitsCount == 0) {
			bitsBuffer = 0;
		}

		long mask = 0;
		for (long i = 0; i < size; i++) {
			mask |= 0x00000001 << i;
		}
		value = value & mask;

		bitsCount += size;
		if (bitsCount < 8) {
			bitsBuffer |= value << (8 - bitsCount);
		} else {
			bitsBuffer |= value;
		}

		if (bitsCount >= 8) {
			writeInt(bitsBuffer, 1);
			bitsCount = 0;
		}

		return 0;
	}

	/**
	 * 写入一个指定的长度的 Float 类型的值
	 * @param floatValue 要写入的 float 类型值.
	 * @param size 这个值的长度, 单位为字节, 当前只支持 4 和 2 两种.
	 * @throws IOException 如果指定的长度不支持, 或者发生写错误
	 */
	public final void writeFloat(float value, int size) throws IOException {
		
		if (size == 4) {
			int iPart = (int)(value) & 0xffff;
			int fPart = (int)((value - iPart) * 0x10000);
			writeInt16(iPart);
			writeInt16(fPart);

		} else if (size == 2) {
			int iPart = ((int)value) & 0xff;
			int fPart = (int)((value - iPart) * 0x100);
			write(iPart);
			write(fPart);

		} else {
			throw new IOException("Invalid float size: " + size);
		}
	}

	/**
	 * 写入一个指定的长度的整数的值.
	 * @param value 要写入的整数的值.
	 * @param size 这个值的长度, 单位为字节, 当前只支持 8, 4, 3, 2 和 1 几种.
	 * @throws IOException 发生写错误
	 */
	public final void writeInt(long value, int size) throws IOException {
		switch (size) {
		case 1: write((int) value);			break;
		case 2: writeInt16((int) value);	break;
		case 3: writeInt24((int) value);	break;
		case 4: writeInt32(value);			break;
		case 8: writeLong(value);			break;
		default: throw new IOException("Invalid integer size: " + size);
		}
	}

	/**
	 * 写入一个 16 位的整数的值.
	 * @param value 要写入的整数的值.
	 * @throws IOException 发生写错误
	 */
	public final void writeInt16(int value) throws IOException {
		write(value >>> 8 & 0xff);
		write(value >>> 0 & 0xff);
	}

	/**
	 * 写入一个 24 位的整数的值.
	 * @param value 要写入的整数的值.
	 * @throws IOException 发生写错误
	 */
	public final void writeInt24(int value) throws IOException {
		write(value >>> 16 & 0xff);
		write(value >>> 8 &  0xff);
		write(value >>> 0 &  0xff);
	}

	/**
	 * 写入一个 32 位的整数的值.
	 * @param value 要写入的整数的值.
	 * @throws IOException 发生写错误
	 */
	public final void writeInt32(long value) throws IOException {
		write((int)(value >>> 24) & 0xff);
		write((int)(value >>> 16) & 0xff);
		write((int)(value >>> 8)  & 0xff);
		write((int)(value >>> 0)  & 0xff);
	}

	/**
	 * 写入一个 64 位的整数的值.
	 * @param value 要写入的整数的值.
	 * @throws IOException 发生写错误
	 */
	public final void writeLong(long value) throws IOException {
		write((int)(value >>> 56) & 0xff);
		write((int)(value >>> 48) & 0xff);
		write((int)(value >>> 40) & 0xff);
		write((int)(value >>> 32) & 0xff);
		write((int)(value >>> 24) & 0xff);
		write((int)(value >>> 16) & 0xff);
		write((int)(value >>> 8)  & 0xff);
		write((int)(value >>> 0)  & 0xff);
	}

	public final void write(ByteBuffer buffer) throws IOException {
		int size = buffer.limit() - buffer.position();
		write(buffer.array(), buffer.position(), size);
	}

	public final void writePaddingBytes(int size) throws IOException {
		int leftover = size;
		byte[] buffer = new byte[1024];
		while (leftover > 0) {
			int len = Math.min(1024, leftover);
			write(buffer, 0, len);
			leftover -= len;
		}
	}
}
