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
 * 
 * 
 * @author ChengZhen(anyou@msn.com)
 */
final class Mp4DescriptorProperty extends Mp4Property {

	private final Mp4Descriptor descriptor;

	public Mp4DescriptorProperty(String name, int type) {
		super(Mp4PropertyType.PT_DESCRIPTOR, 0, name);
		descriptor = new Mp4Descriptor(type);
	}

	@Override
	public String getValueString() {
		return super.getValueString();
	}

	public int getSize() {
		return descriptor.getSize();
	}

	@Override
	public void read(Mp4Stream mp4File) throws IOException {
		int size = (int) getExpectSize();
		if (size <= 0) {
			return;
		}

		byte[] data = new byte[size];
		mp4File.readBytes(data);
		
		setValue(data);

		ByteBuffer buffer = ByteBuffer.wrap(data);
		try {
			descriptor.read(buffer);
		} catch (Exception e) {
			
		}
	}

	@Override
	public void write(Mp4Stream file) throws IOException {
		if (descriptor != null) {
			descriptor.write(file);
		}
	}

	public Mp4Descriptor getDescriptor() {
		return descriptor;
	}
}
