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

import static org.vision.media.mp4.Mp4PropertyType.PT_DESCRIPTOR;
import static org.vision.media.mp4.Mp4PropertyType.PT_SIZE_TABLE;
import static org.vision.media.mp4.Mp4PropertyType.PT_TABLE;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 代表 MP4 文件的一个 Atom(QickTime) 或者 Box(ISO Media File).
 * 
 * @author ChengZhen(anyou@msn.com)
 */
final public class Mp4Atom {

	/** Atom 字符串类型到整型类型. */
	public static int getAtomId(String type) {
		if (type == null || type.isEmpty()) {
			return -1;

		} else if (type.length() < 4) {
			return -1;
		}

		int result = (type.charAt(0) << 24);
		result += (type.charAt(1) << 16);
		result += (type.charAt(2) << 8);
		result += (type.charAt(3));
		return result;
	}

	/** Atom 整型类型到字符串类型. */
	public static String getAtomType(int type) {
		StringBuffer st = new StringBuffer();
		st.append((char) ((type >> 24) & 0xff));
		st.append((char) ((type >> 16) & 0xff));
		st.append((char) ((type >> 8) & 0xff));
		st.append((char) (type & 0xff));
		return st.toString();
	}

	/** 包含这个 Atom 的所有子 Atom 的列表 */
	private final List<Mp4Atom> childAtoms;

	/** 这个 Atom 的深度 */
	private int depth = 0;

	/** 这个 Atom 是否期望有子 Atom */
	private boolean expectChild;

	/** 这个 Atom 的父 Atom */
	private Mp4Atom parentAtom;

	/** 这个 Atom 的属性列表 */
	private final List<Mp4Property> properties;

	/** 这个 Atom 总共的大小, 单位为字节, 包括头部. */
	private long size = 0;

	/** 这个 Atom 在文件中的开始位置, 单位为字节 */
	private long start = 0;

	/** 这个 Atom 的类型, 长 4 个字节. */
	private final int type;

	/**
	 * 构建方法
	 * 
	 * @param type
	 */
	protected Mp4Atom(int type) {
		this.type = type;
		properties = new CopyOnWriteArrayList<Mp4Property>();
		childAtoms = new CopyOnWriteArrayList<Mp4Atom>();
	}

	/**
	 * 添加一个子 Atom
	 * 
	 * @param atom
	 */
	public void addChildAtom(int index, Mp4Atom atom) {
		if (atom == null) {
			return;
		}

		if (index < 0 || index > childAtoms.size()) {
			childAtoms.add(atom);

		} else {
			childAtoms.add(index, atom);
		}
	}

	/**
	 * 添加一个指定的名称的子 Atom
	 * 
	 * @param name
	 *            要添加的 Atom 的名称.
	 * @param index
	 *            要添加的位置
	 * @return 返回新添加的 Atom 的引用.
	 */
	public Mp4Atom addChildAtom(int index, String name) {
		if (Mp4Utils.isBlank(name)) {
			throw new IllegalArgumentException("Blank atom name: " + name);
		}
		String subname = null;
		int pos = name.indexOf('.');
		if (pos > 0) {
			subname = name.substring(pos + 1);
			name = name.substring(0, pos);
		}

		if (Mp4Utils.isNotBlank(subname)) {
			Mp4Atom child = getChildAtom(name);
			if (child == null) {
				child = addChildAtom(name);
			}
			if (child != null) {
				return child.addChildAtom(index, subname);
			}
			return null;

		} else {
			Mp4Atom atom = Mp4Factory.getInstanae().newAtom(name);
			atom.setParentAtom(this);
			atom.setDepth(depth + 1);

			addChildAtom(index, atom);
			return atom;
		}
	}

	/**
	 * 添加一个子 Atom
	 * 
	 * @param atom
	 */
	public void addChildAtom(Mp4Atom atom) {
		if (atom != null) {
			childAtoms.add(atom);
			atom.setParentAtom(this);
			atom.setDepth(depth + 1);
		}
	}

	/**
	 * 添加一个指定的名称的子 Atom.
	 * 
	 * @param name
	 *            要添加的 Atom 的名称.
	 * @return 返回新添加的 Atom 的引用.
	 */
	public Mp4Atom addChildAtom(String name) {
		return addChildAtom(-1, name);
	}

	/**
	 * 添加一个指定的属性
	 */
	public void addProperty(Mp4Property property) {
		properties.add(property);
	}

	/**
	 * 添加一个指定的类型的属性
	 * 
	 * @param type
	 * @param size
	 * @param name
	 * @return
	 */
	public Mp4Property addProperty(Mp4PropertyType type, int size, String name) {
		Mp4Property property;
		if (type == PT_SIZE_TABLE) {
			property = new Mp4SizeTableProperty(name);

		} else if (type == PT_DESCRIPTOR) {
			return null; // 不支持通过这个方法添加这个类型的属性

		} else if (type == PT_TABLE) {
			return null; // 不支持通过这个方法添加这个类型的属性

		} else {
			property = new Mp4Property(type, size, name);
		}

		properties.add(property);
		return property;
	}

	/** 清除所有的内容. */
	public void clear() {
		parentAtom = null;
		properties.clear();
		childAtoms.clear();
		start = 0;
		size = 0;
		depth = 0;
		expectChild = false;
	}

	/**
	 * 清除所有的子节点.
	 */
	public void clearChildAtoms() {
		childAtoms.clear();
	}

	/**
	 * 查找指定的名称的子 Atom 节点.
	 * 
	 * @param name
	 *            Atom 的名称.
	 * @return 返回相应的 Atom 节点.
	 */
	public Mp4Atom findAtom(String name) {
		if (Mp4Utils.isBlank(name)) {
			return null;
		}

		String prefix = null;
		String subname = null;
		int pos = name.indexOf('.');
		if (pos > 0) {
			subname = name.substring(pos + 1);
			prefix = name.substring(0, pos);
		}

		if (subname == null || subname.isEmpty()) {
			return getChildAtom(name);
		}

		Mp4Atom child = getChildAtom(prefix);
		if (child != null) {
			return child.findAtom(subname);
		}

		return null;
	}

	/**
	 * 返回指定的名称的属性.
	 * 
	 * @param name
	 */
	public Mp4Property findProperty(String name) {
		if (Mp4Utils.isBlank(name)) {
			return null;
		}

		String prefix = null;
		String subname = null;
		int pos = name.indexOf('.');
		if (pos > 0) {
			subname = name.substring(pos + 1);
			prefix = name.substring(0, pos);
		}

		if (subname == null || subname.isEmpty()) {
			return getProperty(name);
		}

		Mp4Atom child = getChildAtom(prefix);
		if (child != null) {
			return child.findProperty(subname);
		}

		return null;
	}

	/**
	 * 返回属于当前 Atom 的指定的索引的子 Atom 对象.
	 * 
	 * @param index
	 * @return
	 */
	public Mp4Atom getChildAtom(int index) {
		if (index < 0 || index >= childAtoms.size()) {
			return null;
		}
		return childAtoms.get(index);
	}

	/**
	 * 返回属于当前 Atom 的指定的名称的子 Atom 对象.
	 * 
	 * @param name
	 *            子 Atom 对象的名称
	 * @return
	 */
	protected final Mp4Atom getChildAtom(String name) {
		if (Mp4Utils.isBlank(name)) {
			return null;
		}

		int type = getAtomId(name);
		for (Mp4Atom atom : childAtoms) {
			if (type == atom.getType()) {
				return atom;
			}
		}

		return null;
	}

	/** 返回这个 Atom 的所有子 Atom 对象. */
	public List<Mp4Atom> getChildAtoms() {
		return childAtoms;
	}

	/** 返回这个 Atom 的深度. */
	public int getDepth() {
		return depth;
	}

	/** 返回这个 Atom 在文件中的结束位置. */
	public long getEnd() {
		return start + size;
	}

	/** 返回这个 Atom 的父 Atom 对象. */
	public Mp4Atom getParentAtom() {
		return parentAtom;
	}

	/** 返回这个 Atom 的所有属性. */
	public List<Mp4Property> getProperties() {
		return properties;
	}

	/**
	 * 返回指定的索引的属性.
	 * 
	 * @param index
	 */
	public Mp4Property getProperty(int index) {
		if (index < 0 || index >= properties.size()) {
			return null;
		}
		return properties.get(index);
	}

	/**
	 * 返回指定的名称的属性.
	 * 
	 * @param name
	 */
	public Mp4Property getProperty(String name) {
		if (name == null) {
			return null;
		}

		for (Mp4Property property : properties) {
			if (name.equalsIgnoreCase(property.getName())) {
				return property;
			}
		}

		return null;
	}

	/**
	 * 返回指定的名称的属性的值.
	 * 
	 * @param path
	 */
	public float getPropertyFloat(String path) {
		Mp4Property property = findProperty(path);
		if (property != null) {
			return property.getFloatValue();
		}
		return 0.0f;
	}

	/**
	 * 返回指定的名称的属性的值.
	 * 
	 * @param path
	 */
	public long getPropertyInt(String path) {
		Mp4Property property = findProperty(path);
		if (property != null) {
			return property.getValueInt();
		}
		return 0;
	}

	/**
	 * 返回指定的名称的属性的值.
	 * 
	 * @param path
	 */
	public String getPropertyString(String path) {
		Mp4Property property = findProperty(path);
		if (property != null) {
			return property.getValueString();
		}
		return null;
	}

	/** 返回这个 Atom 总共的长度. */
	public long getSize() {
		return size;
	}

	/** 返回这个 Atom 在文件中的开始位置. */
	public long getStart() {
		return start;
	}

	/** 返回这个 Atom 的类型. */
	public int getType() {
		return type;
	}

	/** 返回这个 Atom 的类型. */
	public String getTypeString() {
		return getAtomType(type);
	}

	/** 指出是否期望有子 Atom 对象. */
	public boolean isExpectChild() {
		return expectChild;
	}

	/** 删除指定 的子 Atom. */
	public void removeAtom(Mp4Atom atom) {
		if (atom != null) {
			childAtoms.remove(atom);
		}
	}

	/** 设置这个 Atom 的深度. */
	public void setDepth(int depth) {
		this.depth = depth;
	}

	/** 设置是否期望有子 Atom 对象. */
	public void setExpectChild(boolean expectChild) {
		this.expectChild = expectChild;
	}

	/** 设置这个 Atom 的父 Atom 对象. */
	public void setParentAtom(Mp4Atom parentAtom) {
		this.parentAtom = parentAtom;
	}

	/**
	 * 设置指定的名称的属性的值.
	 * 
	 * @param name
	 * @param value
	 */
	public void setProperty(String name, float value) {
		Mp4Property property = findProperty(name);
		if (property != null) {
			property.setValue(value);
		}
	}

	/**
	 * 设置指定的名称的属性的值.
	 * 
	 * @param name
	 * @param value
	 */
	public void setProperty(String name, long value) {
		Mp4Property property = findProperty(name);
		if (property != null) {
			property.setValue(value);
		}
	}

	/**
	 * 设置指定的名称的属性的值.
	 * 
	 * @param name
	 * @param value
	 */
	public void setProperty(String name, String value) {
		Mp4Property property = findProperty(name);
		if (property != null) {
			property.setValue(value);
		}
	}

	/** 设置这个 Atom 总共的长度. */
	public void setSize(long size) {
		this.size = size;
	}

	/** 设置这个 Atom 在文件中的开始位置. */
	public void setStart(long start) {
		this.start = start;
	}

	/** 重新计算这个 Atom 的实际大小. */
	protected void updateSize() {
		int sz = 8; // Atom header size
		int bitSize = 0;

		// Properties list size
		for (Mp4Property property : properties) {
			if (property.getType() == Mp4PropertyType.PT_BITS) {
				bitSize += property.getSize();
			} else {
				sz += property.getSize();
			}
		}

		if (bitSize > 0) {
			sz += bitSize / 8;
		}

		// Child atoms size
		for (Mp4Atom atom : childAtoms) {
			atom.updateSize();
			sz += atom.getSize();
		}

		if (size != sz) {
			size = sz;
		}
	}
}
