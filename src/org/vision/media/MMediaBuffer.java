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
package org.vision.media;

import java.nio.ByteBuffer;

/**
 * 代表一个媒体数据块. 它是一个媒体流的基本组成部分.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class MMediaBuffer {

	/** 指出这个数据块是否是一帧的最后一个数据块. */
	public static final int FLAG_END = 0x01;

	/** 指出这个数据块是否是一个同步点. */
	public static final int FLAG_SYNC_POINT = 0x02;

	public static MMediaBuffer wrap(ByteBuffer data, long timeUs, int flags) {
		MMediaBuffer buffer = new MMediaBuffer();
		buffer.setData(data);
		buffer.setSampleTime(timeUs);
		buffer.setFlags(flags);
		return buffer;
	}

	/** 媒体数据内容缓存区. */
	private ByteBuffer data;

	private int flags = 0;
	
	/** 采集这个数据块的时间戳, 单位为微秒 (1/1000000秒). */
	private long sampleTime = 0;
	
	public ByteBuffer getData() {
		return data;
	}

	public int getFlags() {
		return flags;
	}

	/** 采集这个数据块的时间戳, 单位为微秒 (1/1000000秒). */
	public long getSampleTime() {
		return sampleTime;
	}

	public int getSize() {
		if (data == null) {
			return 0;
		}

		return data.limit() - data.position();
	}

	public boolean isEnd() {
		return (flags & FLAG_END) != 0;
	}

	public boolean isSyncPoint() {
		return (flags & FLAG_SYNC_POINT) != 0;
	}

	public void setData(ByteBuffer data) {
		this.data = data;
	}

	public void setEnd(boolean isEnd) {
		if (isEnd) {
			flags |= FLAG_END;
			
		} else {
			flags &= ~FLAG_END;
		}
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public void setSampleTime(long timestamp) {
		this.sampleTime = timestamp;
	}

	public void setSyncPoint(boolean isSyncPoint) {
		if (isSyncPoint) {
			flags |= FLAG_SYNC_POINT;
			
		} else {
			flags &= ~FLAG_SYNC_POINT;
		}
	}

	public String toString() {
		String text = "{";
		text += isEnd() ? "end," : "";
		text += isSyncPoint() ? "sync," : "";
		text += "size:" + getSize();
		text += ",time:" + getSampleTime();
		text += "}";
		return text;
	}
}
