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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 代表一个 MP4 样本.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
final public class Mp4Sample {

	private List<ByteBuffer> buffers;

	/**
	 * 是否是一个同步点, 比如 I 帧 的第一个字节.
	 * 
	 * The sample is a sync sample
	 */
	private boolean isSyncPoint = false;
	
	private boolean isEnd = false;

	/** 这个 Sample 的持续时间. */
	private long sampleDuration = -1;

	/** 这个 sample 的 ID. */
	private int sampleId = -1;

	/** 这个 sample 的 大小. */
	private long sampleSize;

	/**
	 * 时间戳, 单位为微秒 (1/100,0000 秒) Returns the current sample's presentation time
	 * in microseconds. or -1 if no more samples are available.
	 */
	private long sampleTime;

	/** 这个 Sample 所属的 Track 的 Id. */
	private int trackIndex = -1;

	public Mp4Sample() {

	}

	/**
	 * 构建方法
	 * 
	 * @param sampleId
	 *            这个样本的 ID.
	 * @param data
	 *            这个样本的数据内容.
	 */
	public Mp4Sample(int sampleId, byte[] data) {
		this.sampleId = sampleId;

		if (data != null) {
			addData(ByteBuffer.wrap(data));
		}
	}

	public void addData(ByteBuffer data) {
		if (data == null) {
			return;
		}

		if (buffers == null) {
			buffers = new ArrayList<ByteBuffer>();
		}

		buffers.add(data);

		int size = data.limit() - data.position();
		sampleSize += size;
	}

	public void addDataAndHeader(ByteBuffer data) {
		int size = data.limit() - data.position();
		addData(newSizeHeader(size));
		addData(data);
	}

	public List<ByteBuffer> getBuffers() {
		return buffers;
	}

	public ByteBuffer getData() {
		if (buffers == null || buffers.isEmpty()) {
			return null;
		}

		return buffers.get(0);
	}

	/** 这个 Sample 的持续时间. */
	public long getSampleDuration() {
		return sampleDuration;
	}

	/** 这个 sample 的 ID. */
	public int getSampleId() {
		return sampleId;
	}

	public long getSampleSize() {
		return sampleSize;
	}

	public long getSampleTime() {
		return sampleTime;
	}

	/** 这个 Sample 所属的 Track 的 Id. */
	public int getTrackIndex() {
		return trackIndex;
	}

	public boolean isSyncPoint() {
		return isSyncPoint;
	}

	public ByteBuffer newSizeHeader(int size) {
		ByteBuffer header = ByteBuffer.allocate(4);
		header.putInt(size);
		header.position(0);
		return header;
	}

	/** 这个 Sample 的持续时间. */
	public void setSampleDuration(long duration) {
		this.sampleDuration = duration;
	}

	/** 这个 sample 的 ID. */
	public void setSampleId(int sampleId) {
		this.sampleId = sampleId;
	}

	public void setSampleSize(long sampleSize) {
		this.sampleSize = sampleSize;
	}

	public void setSampleTime(long sampleTime) {
		this.sampleTime = sampleTime;
	}

	public void setSyncPoint(boolean isSyncPoint) {
		this.isSyncPoint = isSyncPoint;
	}

	/** 这个 Sample 所属的 Track 的 Id. */
	public void setTrackIndex(int trackIndex) {
		this.trackIndex = trackIndex;
	}

	public boolean isEnd() {
		return isEnd;
	}

	public void setEnd(boolean isEnd) {
		this.isEnd = isEnd;
	}
}