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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代表一个文件中的 MP4 流.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class Mp4FileStream extends Mp4Stream {

	private static final Logger log = LoggerFactory.getLogger(Mp4FileStream.class);

	/** 相关的文件. */
	private RandomAccessFile file;

	/** 当前读写模式. */
	private boolean isReadOnly = false;

	public Mp4FileStream() {
	}
	
	public Mp4FileStream(File file, boolean isReadOnly) throws IOException {
		if (file != null) {
			open(file, isReadOnly);
		}
	}

	/**
	 * 构建方法
	 * @param isWriting 
	 * @param filename 
	 * @throws IOException 
	 */
	public Mp4FileStream(String filename, boolean isReadOnly) throws IOException {
		if (filename != null) {
			open(filename, isReadOnly);
		}
	}
	
	/**
	 * 关闭当前打开的文件
	 */
	public void close() {
		super.close();
		
		if (file != null) {
			try {
				file.close();
			} catch (IOException e) {
			} finally {
				file = null;
			}
		}
	}
	
	public void flush() {
		try {
			FileDescriptor fd = (file == null) ? null : file.getFD();
			if (fd != null) {
				fd.sync();
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 返回当前打开的文件的长度.
	 * @return
	 */
	public long getFileSize() {
		try {
			return (file == null) ? 0 : file.length();
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * 返回当前文件指针的位置
	 * @return
	 */
	public long getPosition() {
		try {
			return (file == null) ? -1 : file.getFilePointer();
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
			return -1;
		}
	}

	/**
	 * 指出文件是否是打开的
	 * @return
	 */
	public boolean isOpen() {
		return (file != null);
	}

	/**
	 * 指出是否是在写文件
	 * @return
	 */
	public boolean isReadOnly() {
		return isReadOnly;
	}

	private final void open(File file, boolean isReadOnly) throws IOException {
		this.isReadOnly = isReadOnly;
		if (isReadOnly) {
			if (!file.exists()) {
				throw new FileNotFoundException(file.getAbsolutePath());
			}
		}
		
		this.file = new RandomAccessFile(file, isReadOnly ? "r" : "rw");
	}
	
	/**
	 * 打开指定名称的文件
	 * @param filename
	 * @param isWriting
	 * @throws IOException
	 */
	private final void open(String filename, boolean isReadOnly) throws IOException {
		File file = new File(filename);
		open(file, isReadOnly);
	}
	
	/**
	 * 读取一个字节
	 */
	public int read() throws IOException {
		return file.read();
	}

	/**
	 * 批量读取指定个字节
	 * @param type
	 * @return
	 * @throws IOException
	 */
	public int readBytes(byte[] dst) throws IOException {
		file.readFully(dst);
		return dst.length;
	}

	@Override
	public int readBytes(byte[] dst, int offset, int length) throws IOException {
		file.readFully(dst, offset, length);
		return length;
	}

	/**
	 * 定位到指定的位置.
	 * @param position
	 * @throws IOException
	 */
	public void seek(long position) throws IOException {
		if (file != null) {
			file.seek(position);
		}
	}

	/**
	 * 跳过指定个字节
	 * @param size
	 * @throws IOException
	 */
	public void skip(int size) throws IOException {
		file.skipBytes(size);
	}

	/**
	 * 批量写入多个 byte
	 * @param bytes 要写入的多个 byte 的值.
	 * @throws IOException 发生写错误
	 */
	public void write(byte[] bytes) throws IOException {
		file.write(bytes);
	}

	public void write(byte[] bytes, int offset, int len) throws IOException {
		if (file == null) {
			return;
		}
		file.write(bytes, offset, len);
	}

	/**
	 * 写入一个 byte.
	 * @param value 要写入的 byte 的值.
	 * @throws IOException 发生写错误
	 */
	public void write(int value) throws IOException {
		file.write(value);
	}
}
