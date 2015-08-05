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
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 代表一个 Atom 表格属性
 * 
 * @author ChengZhen(anyou@msn.com)
 */
final class Mp4TableProperty extends Mp4Property {

	/** 这个表格的所有字段. */
	private List<String> fields = new ArrayList<String>();

	/** 这个表格的所有行. */
	private List<long[]> rows = new ArrayList<long[]>();

	/**
	 * 构建方法
	 * 
	 * @param name
	 *            这个属性的名称
	 */
	public Mp4TableProperty(String name) {
		super(Mp4PropertyType.PT_TABLE, -1, name);
	}

	/**
	 * 添加一列. 注意如果表格中已经添加数据了就不能再添加列了.
	 * 
	 * @param name
	 *            要添加的列的名称.
	 * @throws IllegalStateException
	 *             如果这个表格已经有数据了
	 */
	public void addColumn(String name) {
		if (!rows.isEmpty()) {
			throw new IllegalStateException("The table is now empty.");
		}
		fields.add(name);
	}

	/**
	 * 添加新的一行
	 * 
	 * @param line
	 *            要添加数据.
	 */
	public void addRow(long... line) {
		int colCount = getFieldCount(); // 表格列数
		long[] row = new long[colCount];
		for (int col = 0; (col < colCount) && (col < line.length); col++) {
			row[col] = line[col];
		}
		rows.add(line);
	}

	/** 返回当前表格的列数. */
	public int getFieldCount() {
		return fields.size();
	}

	/**
	 * 返回指定的索引的字段的名称.
	 * 
	 * @param index
	 *            字段的索引
	 * @return 指定的索引的字段的名称.
	 */
	public String getFieldName(int index) {
		return fields.get(index);
	}

	/**
	 * 返回最后一行.
	 * 
	 * @return 如果不为空, 返回表格的最后一行, 否则返回 null.
	 */
	public long[] getLastRow() {
		return rows.isEmpty() ? null : rows.get(rows.size() - 1);
	}

	/**
	 * 返回指定的行.
	 * 
	 * @param index
	 *            要返回的行的索引值
	 * @return 指定的行.
	 */
	public long[] getRow(int index) {
		if ((index >= 0) && (index < rows.size())) {
			return rows.get(index);
		}
		return null;
	}

	/** 返回表格当前的行数. */
	public int getRowCount() {
		return rows.size();
	}

	/**
	 * 返回这个表格的所有行
	 * 
	 * @return 这个表格的所有行
	 */
	public List<long[]> getRows() {
		return rows;
	}

	@Override
	public int getSize() {
		return rows.size() * getFieldCount() * 4;
	}

	public String getTableString() {
		StringBuilder sb = new StringBuilder();
		int count = rows != null ? rows.size() : 0;
		for (int i = 0; i < count; i++) {
			long[] row = rows.get(i);
			sb.append(i).append(": ");

			for (long value : row) {
				sb.append(value);
				sb.append(",");
			}
			sb.append("\r\n");
		}

		return sb.toString();
	}

	/**
	 * 返回指定的行指定的列的值.
	 * 
	 * @param row
	 *            行
	 * @param col
	 *            列
	 * @return 指定的行指定的列的值, 如果不存在则返回 -1.
	 */
	public long getValue(int row, int col) {
		long[] line = getRow(row);
		if (line == null) {
			return -1;
		}
		if ((col >= 0) && (col < line.length)) {
			return line[col];
		}
		return -1;
	}

	/**
	 * 字符串值.
	 */
	@Override
	public String getValueString() {
		return "table";
	}

	/**
	 * 指出这个表格是否为空.
	 * 
	 * @return 返回 true 如果这个表格为空.
	 */
	public boolean isEmpty() {
		return rows.isEmpty();
	}

	/**
	 * 读取表格的内容
	 */
	@Override
	public void read(Mp4Stream mp4File) throws IOException {
		int colCount = getFieldCount(); // 表格列数
		long rowCount = getExpectSize(); // 表格行数

		int size = colCount * (int) rowCount * 4;

		byte[] data = new byte[size];
		mp4File.readBytes(data);

		ByteBuffer buffer = ByteBuffer.wrap(data);
		IntBuffer array = buffer.asIntBuffer();

		// 读取每行每列的值
		for (int row = 0; row < rowCount; row++) {
			long[] line = new long[colCount];
			for (int col = 0; col < colCount; col++) {
				line[col] = array.get();
			}
			rows.add(line);
		}
	}

	/**
	 * 写入指定的文件
	 */
	@Override
	public void write(Mp4Stream mp4File) throws IOException {
		int colCount = getFieldCount(); // 表格列数
		long rowCount = getRowCount(); // 表格行数

		// 读取每行每列的值
		for (int row = 0; row < rowCount; row++) {
			long[] line = rows.get(row);
			for (int col = 0; col < colCount; col++) {
				mp4File.writeInt32(line[col]);
			}
		}
	}

}
