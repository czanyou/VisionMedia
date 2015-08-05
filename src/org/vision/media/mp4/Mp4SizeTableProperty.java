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
import java.util.ArrayList;
import java.util.List;

/**
 * 代表一个 byte 数组表格.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
final class Mp4SizeTableProperty extends Mp4Property {

	/** 这个表格的所有行. */
	private List<byte[]> rows = new ArrayList<byte[]>();

	/**
	 * 构建方法
	 * @param name
	 */
	public Mp4SizeTableProperty(String name) {
		super(Mp4PropertyType.PT_SIZE_TABLE, -1, name);
	}

	/**
	 * 添加一个实体
	 * @param bytes
	 */
	public void addEntry(byte[] bytes) {
		rows.add(bytes);
	}

	public byte[] getEntry(int index) {
		return rows.get(index);
	}

	/**
	 * 返回这个表格当前的行数
	 * @return
	 */
	public int getRowCount() {
		return rows.size();
	}

	/**
	 * 返回这个表格的所有行
	 * @return
	 */
	public List<byte[]> getRows() {
		return rows;
	}

	@Override
	public int getSize() {
		int sz = 0;
		for (byte[] data : rows) {
			sz += 2;
			sz += data.length;
		}
		return sz;
	}

	/**
	 * 返回代表这个表格的字符串值
	 */
	@Override
	public String getValueString() {
		return "[size-table]";
	}

	/**
	 * 读取这个表格
	 */
	@Override
	public void read(Mp4Stream mp4File) throws IOException {
		long count = getExpectSize();	// 表格行数
		for (int i = 0; i < count; i++) {
			int length = mp4File.readInt16();
			byte bytes[] = new byte[length];
			mp4File.readBytes(bytes);
			addEntry(bytes);
		}
	}

	/**
	 * 写入这个表格
	 */
	@Override
	public void write(Mp4Stream mp4File) throws IOException {
		long count = getRowCount();	// 表格行数
		for (int i = 0; i < count; i++) {
			byte bytes[] = rows.get(i);
			mp4File.writeInt16(bytes.length);
			mp4File.write(bytes);
		}
	}
}
