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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 代表一个内存中的 MP4 流.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
class Mp4MemStream extends Mp4Stream {
	public ByteBuffer buffer;
	
	public Mp4MemStream(int size)
			throws IOException {
		
		buffer = ByteBuffer.allocate(size);
	}

	@Override
	public long getFileSize() {
		return buffer.position();
	}
	
	public long getPosition() {
		return buffer.position();
	}

	@Override
	public boolean isOpen() {
		return true;
	}
	
	public int read() throws IOException {
		return buffer.get() & 0xFF;
	}
	
	public int readBytes(byte[] dst) throws IOException {
		buffer.get(dst);
		return dst.length;
	}
	
	@Override
	public int readBytes(byte[] dst, int offset, int length) throws IOException {
		buffer.get(dst, offset, length);
		return length;
	}

	public void seek(long position) throws IOException {
		buffer.position((int)position);
	}
	
	public int size() {
		return buffer.position();
	}
	
	public void skip(int size) throws IOException {
		buffer.position(buffer.position() + size);
	}
	
	@Override
	public void write(byte[] bytes) throws IOException {
		buffer.put(bytes);
	}

	public void write(byte[] bytes, int offset, int len) throws IOException {
		buffer.put(bytes, offset, len);
	}

	@Override
	public void write(int value) throws IOException {
		buffer.put((byte)(value & 0xFF));
	}

}