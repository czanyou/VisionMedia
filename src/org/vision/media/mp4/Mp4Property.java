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
import java.text.DateFormat;
import java.util.Date;

/**
 * 代表一个 MP4 文件的 Atom 节点的属性.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class Mp4Property {

	/** 这个属性期望的大小或长度. */
	private long expectSize;

	/** 这个属性的名称. */
	private String name;

	/** 这个属性的值的长度, 默认单位为字节, Bits 类型属性单位为位(bit). */
	private int size;

	/** 这个属性的类型. */
	private final Mp4PropertyType type;

	/** 这个属性的值 */
	private Object value;

	/**
	 * 默认构造方法
	 * 
	 * @param type
	 *            这个属性的类型
	 */
	public Mp4Property(Mp4PropertyType type) {
		this.type = type;
	}

	/**
	 * 构造方法
	 * 
	 * @param type
	 *            这个属性的类型
	 * @param size
	 *            这个属性的值的长度
	 * @param name
	 *            这个属性的显示名称.
	 */
	public Mp4Property(Mp4PropertyType type, int size, String name) {
		this.type = type;
		this.size = size;
		this.name = name;
	}

	protected byte[] getBytes() {
		byte[] data = null;
		if ((value != null) && (value instanceof byte[])) {
			data = (byte[]) value;

		} else if (size > 0) {
			data = new byte[size];
		}
		return data;
	}

	/** 返回期望的大小或长度. */
	public long getExpectSize() {
		return expectSize;
	}

	/** 返回这个属性的浮点型值. */
	public float getFloatValue() {
		if (value == null) {
			return 0.0f;
		} else if (value instanceof Number) {
			return ((Number) value).floatValue();
		} else {
			return 0.0f;
		}
	}

	/** 返回这个属性的名称. */
	public String getName() {
		return name;
	}

	/** 返回这个属性的值的长度 */
	public int getSize() {
		return size;
	}

	/** 返回这个属性的类型. */
	public Mp4PropertyType getType() {
		return type;
	}

	/** 返回这个属性的值. */
	public Object getValue() {
		return this.value;
	}

	/** 返回这个属性的整型值. */
	public long getValueInt() {
		if (value == null) {
			return 0;

		} else if (value instanceof Number) {
			return ((Number) value).longValue();

		} else {
			return 0;
		}
	}

	/** 返回这个属性的字符串类型值. */
	public String getValueString() {
		if (type == Mp4PropertyType.PT_DATE) {
			Date date = Mp4Factory.getDate(getValueInt());
			int style = DateFormat.MEDIUM;
			return DateFormat.getDateTimeInstance(style, style).format(date);
		}

		if (value instanceof byte[]) {
			if (type == Mp4PropertyType.PT_STRING) {
				byte[] bytes = (byte[]) value;
				byte[] data = new byte[bytes.length];
				for (int i = 0; i < data.length; i++) {
					if (bytes[i] > 30) {
						data[i] = bytes[i];
					} else {
						data[i] = '?';
					}
				}
				return new String(data);

			} else {
				return Mp4Utils.encodeHex((byte[]) value);
			}
		}
		return String.valueOf(value);
	}

	/**
	 * 读取这个属性的值
	 * 
	 * @param mp4File
	 *            要读取的文件
	 * @throws IOException
	 *             如果发生错误
	 */
	public void read(Mp4Stream mp4File) throws IOException {
		if (type == null) {
			throw new IOException("Unknow property type.");
		}

		switch (type) {
		case PT_INT:
			setValue(mp4File.readInt(size));
			break;

		case PT_BITS:
			setValue(mp4File.readBits(size));
			break;

		case PT_DATE:
			setValue(mp4File.readInt32());
			break;

		case PT_FLOAT:
			setValue(mp4File.readFloat(size));
			break;

		case PT_STRING:
		case PT_BYTES: {
			if (size > 0) {
				byte data[] = new byte[size];
				mp4File.readBytes(data);
				this.value = data;
			}
		}
			break;

		case PT_TABLE:
		case PT_DESCRIPTOR:
		case PT_SIZE_TABLE:
			break;
		}
	}

	/** 设置期望的大小或长度. */
	public void setExpectSize(long expectSize) {
		this.expectSize = expectSize;
	}

	/** 设置这个属性的名称. */
	public void setName(String name) {
		this.name = name;
	}

	/** 设置这个属性的值的长度 */
	public void setSize(int size) {
		this.size = size;
	}

	/** 设置这个属性的值 */
	public void setValue(byte[] bytes) {
		if (bytes == null) {
			value = null;
			return;
		}

		byte[] data = new byte[size];
		int length = Math.min(data.length, bytes.length);
		System.arraycopy(bytes, 0, data, 0, length);
		value = data;
	}

	/** 设置这个属性的浮点型值. */
	public void setValue(float floatValue) {
		if (type == Mp4PropertyType.PT_INT || type == Mp4PropertyType.PT_DATE
				|| type == Mp4PropertyType.PT_BITS) {
			this.value = new Long((long) floatValue);

		} else if (type == Mp4PropertyType.PT_FLOAT) {
			this.value = new Float(floatValue);

		} else {
			this.value = null;
		}
	}

	/** 设置这个属性的整型值. */
	public void setValue(long intValue) {
		if (type == Mp4PropertyType.PT_INT || type == Mp4PropertyType.PT_DATE
				|| type == Mp4PropertyType.PT_BITS) {
			this.value = new Long(intValue);

		} else if (type == Mp4PropertyType.PT_FLOAT) {
			this.value = new Float(intValue);

		} else {
			this.value = null;
		}
	}

	public final void setValue(Object value) {
		if (value == null) {
			this.value = null;
			return;
		}

		if (type == Mp4PropertyType.PT_INT || type == Mp4PropertyType.PT_DATE
				|| type == Mp4PropertyType.PT_BITS) {
			if (value instanceof Number) {
				this.value = ((Number) value).longValue();
			} else {
				this.value = null;
			}
		} else if (type == Mp4PropertyType.PT_FLOAT) {
			if (value instanceof Number) {
				this.value = ((Number) value).floatValue();
			} else {
				this.value = null;
			}
		} else {
			if (value instanceof byte[]) {
				this.value = value;

			} else {
				String text = String.valueOf(value);
				setValue(text);
			}
		}
	}

	/** 设置这个属性的字符串类型值. */
	public void setValue(String value) {
		byte[] data = (value == null) ? null : value.getBytes();
		if (size <= 0) {
			size = data.length;
		}
		setValue(data);
	}

	/**
	 * д���������
	 * 
	 * @param file
	 *            Ҫд����ļ�
	 * @throws IOException
	 *             ��������
	 */
	public void write(Mp4Stream file) throws IOException {
		if (type == null) {
			throw new IOException("Unknow property type.");
		}

		switch (type) {
		case PT_INT:
			file.writeInt(getValueInt(), size);
			break;

		case PT_BITS:
			file.writeBits(getValueInt(), size);
			break;

		case PT_DATE:
			file.writeInt32(getValueInt());
			break;

		case PT_FLOAT:
			file.writeFloat(getFloatValue(), size);
			break;

		case PT_STRING:
		case PT_BYTES:
			byte[] data = getBytes();
			if (data != null) {
				file.write(data);
			}
			break;
		case PT_TABLE:
		case PT_DESCRIPTOR:
		case PT_SIZE_TABLE:
			// 默认没有实现
			break;
		}
	}

}
