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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * 
 * @author ChengZhen(anyou@msn.com)
 */
final class Mp4Descriptor {
	// private static final Logger log = LoggerFactory
	// .getLogger(Mp4Descriptor.class);

	public static final int Mp4ContentIdDescrTag = 0x07;
	public static final int Mp4DecConfigDescrTag = 0x04;
	public static final int Mp4DecSpecificDescrTag = 0x05;
	public static final int Mp4ESDescrTag = 0x03;
	public static final int Mp4ESIDIncDescrTag = 0x0E;
	public static final int Mp4ESIDRefDescrTag = 0x0F;
	public static final int Mp4ExtDescrTagsEnd = 0xFE;
	public static final int Mp4ExtDescrTagsStart = 0x80;
	public static final int Mp4ExtProfileLevelDescrTag = 0x13;
	public static final int Mp4FileIODescrTag = 0x10;
	public static final int Mp4FileODescrTag = 0x11;
	public static final int Mp4IODescrTag = 0x02;
	public static final int Mp4IPIPtrDescrTag = 0x09;
	public static final int Mp4IPMPDescrTag = 0x0B;
	public static final int Mp4IPMPPtrDescrTag = 0x0A;
	public static final int Mp4ODescrTag = 0x01;
	public static final int Mp4RegistrationDescrTag = 0x0D;
	public static final int Mp4SLConfigDescrTag = 0x06;
	public static final int Mp4SupplContentIdDescrTag = 0x08;

	private int channelConfiguration = 2;

	private int objectTypeId = 2;

	/** 属性列表 */
	private final List<Mp4Property> properties = new CopyOnWriteArrayList<Mp4Property>();

	private int samplingFreqIndex = 4;

	/** 类型. */
	private final int type;

	/**
	 * 构建方法
	 * 
	 * @param type
	 */
	public Mp4Descriptor(int type) {
		this.type = type;
		init(type);
	}

	public int getChannelConfiguration() {
		return channelConfiguration;
	}

	public int getObjectTypeId() {
		return objectTypeId;
	}

	/**
	 * 返回属性列表
	 * 
	 * @return
	 */
	public List<Mp4Property> getProperties() {
		return properties;
	}

	public int getSamplingFreqIndex() {
		return samplingFreqIndex;
	}

	public int getSize() {
		if (type == Mp4ESDescrTag) {
			return 0x22 + 5;

		} else if (type == Mp4FileIODescrTag) {
			return 12; // 1 + 4 + 7
		}
		return 4;
	}

	/**
	 * 
	 * @param type
	 */
	protected void init(int type) {
		if (type == Mp4ESDescrTag) {
			Mp4PropertyType intType = Mp4PropertyType.PT_INT;
			properties.add(new Mp4Property(intType, 1, "objectTypeId"));
			properties.add(new Mp4Property(intType, 1, "streamType"));
			properties.add(new Mp4Property(intType, 3, "bufferSize"));
			properties.add(new Mp4Property(intType, 4, "maxBitrate"));
			properties.add(new Mp4Property(intType, 4, "avgBitrate"));
		}
	}

	public void read(ByteBuffer buffer) {
		if (type == Mp4ESDescrTag) {
			int tag = buffer.get();
			@SuppressWarnings("unused")
			int size = readHeaderLength(buffer);
			if (tag != Mp4ESDescrTag) {
				return;
			}

			buffer.getShort(); // ES ID
			int flag = buffer.get(); // flags
			if ((flag & 0x80) != 0) { // streamDependenceFlag

			}

			if ((flag & 0x40) != 0) { // URL_Flag

			}

			if ((flag & 0x20) != 0) { // OCRstreamFlag

			}

			tag = buffer.get();
			size = readHeaderLength(buffer);
			if (tag != Mp4DecConfigDescrTag) {
				return;
			}

			buffer.get(); // object type
			buffer.getInt();
			buffer.getInt();
			buffer.getInt();

			if (!buffer.hasRemaining()) {
				return;
			}

			tag = buffer.get();
			size = readHeaderLength(buffer);
			if (tag != Mp4DecSpecificDescrTag) {
				return;
			}

			@SuppressWarnings("unused")
			int kSamplingRate[] = { 96000, 88200, 64000, 48000, 44100, 32000,
					24000, 22050, 16000, 12000, 11025, 8000, 7350, 0, 0, 0 };

			int value = buffer.getShort();
			int objectType = (value >> 11);
			int freqIndex = (value >> 7) & 0x0f;
			int channels = (value >> 3) & 0x0f;

			setObjectTypeId(objectType);
			setSamplingFreqIndex(freqIndex);
			setChannelConfiguration(channels);
		}
	}

	public int readHeaderLength(ByteBuffer buffer) {
		int size = 0;

		while (buffer.hasRemaining()) {
			int data = buffer.get();

			size = (size << 8) + (data & 0x7f);

			if ((data & 0x80) == 0) {
				break;
			}
		}

		return size;
	}

	public void setChannelConfiguration(int channelConfiguration) {
		this.channelConfiguration = channelConfiguration;
	}

	public void setObjectTypeId(int objectTypeId) {
		this.objectTypeId = objectTypeId;
	}

	public void setSamplingFreqIndex(int samplingFreqIndex) {
		this.samplingFreqIndex = samplingFreqIndex;
	}

	/**
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void write(Mp4Stream file) throws IOException {
		if (type == Mp4ESDescrTag) {
			writeAacSbrEsds(file);

		} else if (type == Mp4FileIODescrTag) {
			file.write(Mp4FileIODescrTag); // Mp4FileIODescrTag
			writeMpegLength(file, 0x07);
			file.write(0x00);
			file.write(0x4f);
			file.write(0xff);
			file.write(0xff);

			file.write(0x0f);
			file.write(0x7f);
			file.write(0xff);
		}
	}

	public void writeAacEsds(Mp4Stream file) throws IOException {
		file.write(Mp4ESDescrTag); // Mp4ESDescrTag

		writeMpegLength(file, 0x22);

		// ESID
		file.write(0x00);
		file.write(0x00);

		// flags
		file.write(0x00);

		file.write(Mp4DecConfigDescrTag); // Mp4DecConfigDescrTag
		writeMpegLength(file, 0x14);
		file.write(0x40); // MP4_MPEG4_AUDIO_TYPE
		file.write(0x15); // MP4_AUDIOSTREAMTYPE

		file.writeInt24(0x00);
		file.writeInt32(0x00);
		file.writeInt32(0x00);

		file.write(Mp4DecSpecificDescrTag); // Mp4DecSpecificDescrTag
		writeMpegLength(file, 0x02);
		file.write(0x11);
		file.write(0x90);

		file.write(Mp4SLConfigDescrTag); // Mp4SLConfigDescrTag
		writeMpegLength(file, 0x01);
		file.write(0x02);

	}

	public void writeAacSbrEsds(Mp4Stream file) throws IOException {
		file.write(Mp4ESDescrTag); // Mp4ESDescrTag

		writeMpegLength(file, 0x19);

		// ESID
		file.write(0x00);
		file.write(0x00);

		// flags
		file.write(0x00);

		file.write(Mp4DecConfigDescrTag); // Mp4DecConfigDescrTag
		writeMpegLength(file, 0x11);
		file.write(0x40); // MP4_MPEG4_AUDIO_TYPE
		file.write(0x15); // MP4_AUDIOSTREAMTYPE

		file.writeInt24(0x00);
		file.writeInt32(0x00);
		file.writeInt32(0x00);

		file.write(Mp4DecSpecificDescrTag); // Mp4DecSpecificDescrTag
		writeMpegLength(file, 0x02);

		int value = (objectTypeId << 3) | (samplingFreqIndex >> 1);
		file.write(value);
		value = ((samplingFreqIndex << 7) & 0x80) | (channelConfiguration << 3);
		file.write(value);

		file.write(Mp4SLConfigDescrTag); // Mp4SLConfigDescrTag
		writeMpegLength(file, 0x01);
		file.write(0x02);
	}

	/**
	 * 长度
	 * 
	 * @param file
	 * @param length
	 * @throws IOException
	 */
	private void writeMpegLength(Mp4Stream file, int length) throws IOException {
		// file.write(0x80);
		// file.write(0x80);
		// file.write(0x80);
		file.write(length);
	}

	public void setSamplingFrequency(int timeScale) {
		int kSamplingRate[] = { 96000, 88200, 64000, 48000, 44100, 32000,
				24000, 22050, 16000, 12000, 11025, 8000, 7350, 0, 0, 0 };

		// log.debug("timeScale:" + timeScale);

		for (int i = 0; i < 15; i++) {
			if (kSamplingRate[i] == timeScale) {
				samplingFreqIndex = i;
				// log.debug("samplingFreqIndex:" + samplingFreqIndex);
				break;
			}
		}
	}

}
