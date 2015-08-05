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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Attributes class represents a persistent set of attributes.
 * <p>
 * 
 * @author chengzhen (anyou@msn.com)
 */
public class Mp4QueryString implements Cloneable {

	/** The map containing all attributes. */
	private Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	public Mp4QueryString() {
	}

	public Mp4QueryString(String text) {
		if (text == null) {
			return;
		}
		String tokens[] = text.split("&");
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			int pos = token.indexOf("=");
			if (pos > 0) {
				String name = token.substring(0, pos).trim();
				String value = token.substring(pos + 1).trim();

				setAttribute(name, value);
			}
		}
	}

	/**
	 * Removes all attributes.
	 */
	public void clearAttributes() {
		attributes.clear();
	}

	/**
	 * Returns true if this context contains the specified attributes.
	 * 
	 * @param name
	 * @return
	 */
	public boolean containsAttribute(String name) {
		return attributes.containsKey(name);
	}

	/**
	 * Returns the attribute with the given name, or null if there is no
	 * attribute by that name.
	 */
	public Object getAttribute(String name) {
		if (name == null) {
			return null;
		}

		return attributes.get(name);
	}

	/**
	 * Returns an Iterator containing the attribute names available within this
	 * context.
	 */
	public Iterator<String> getAttributeNames() {
		return attributes.keySet().iterator();
	}

	public int optInt(String name, int defaultValue) {
		Object value = getAttribute(name);
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Number) {
			return ((Number) value).intValue();
		}

		try {
			return Integer.valueOf(value.toString());
		} catch (Exception e) {

		}

		return defaultValue;
	}

	public long optLong(String name, int defaultValue) {
		Object value = getAttribute(name);
		if (value == null) {
			return defaultValue;
		}

		if (value instanceof Number) {
			return ((Number) value).longValue();
		}

		try {
			return Long.valueOf(value.toString());
		} catch (Exception e) {

		}

		return defaultValue;
	}

	public String optString(String name) {
		Object value = getAttribute(name);
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}

	/** Removes the attribute with the given name from the context. */
	public void removeAttribute(String name) {
		if (name == null) {
			return;
		}

		attributes.remove(name);
	}

	/** Binds an object to a given attribute name in this context. */
	public void setAttribute(String name, Object value) {
		if ((name == null) || (value == null)) {
			return;
		}

		attributes.put(name, value);
	}

	public String toQueryString() {
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			sb.append(sep);
			sb.append(entry.getKey()).append("=").append(entry.getValue());
			
			sep = "&";
		}
		return sb.toString();
	}
}
