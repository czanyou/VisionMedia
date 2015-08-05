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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 代表一个 MP4 Chunk, MP4 Chunk 是一组 MP4 Sample 的集合.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
final public class Mp4Chunk {

	/** 这个 Chunk 的 Id. */
	private int chunkId;

	/** 这个 Chunk 在文件中的偏移位置 */
	private long chunkOffset;

	/** 这个 Chunk 缓存中媒体数据总共的时间长度, 单位为微秒. */
	private long duration;

	/** 这个 Chunk 所属的格式 */
	private int payload;

	/** 这个 Chunk 包含的 Sample 的数目. */
	private int sampleCount;

	/** 这个 Chunk 包含的 Sample 的列表 */
	private ArrayList<Mp4Sample> samples = new ArrayList<Mp4Sample>();

	/** 当前 Chunk 缓存中媒体数据总共的长度, 单位为字节. */
	private int size;

	/**
	 * 构建方法
	 */
	public Mp4Chunk() {

	}

	/**
	 * 添加一个新的 Sample.
	 * 
	 * @param buffers
	 *            Sample 的内容
	 * @param sampleDuration
	 *            Sample 的时间长度
	 * @return 返回这个 Sample 总共的长度.
	 */
	public void addSample(Mp4Sample sample) {
		samples.add(sample);

		this.size += sample.getSampleSize();
		this.duration += sample.getSampleDuration();
	}

	/** 清除这个 Sample 的内容. */
	public void clear() {
		if (samples != null) {
			samples.clear();
		}

		size = 0;
		duration = 0;
		sampleCount = 0;
	}

	public long getAvgDuration() {
		int count = getSampleCount();
		if (count <= 0) {
			return 0;
		}
		return duration / count;
	}

	public int getChunkId() {
		return chunkId;
	}

	public long getChunkOffset() {
		return chunkOffset;
	}

	/** 返回这个 Chunk 的时间长度. */
	public long getDuration() {
		return duration;
	}

	public Mp4Sample getLastSample() {
		if (samples == null) {
			return null;
		}

		int count = samples.size();
		return samples.get(count - 1);
	}

	public int getPayload() {
		return payload;
	}

	/* 返回这个 Chunk 的 Sample 的数量. */
	public int getSampleCount() {
		if (samples.size() > 0) {
			return samples.size();
		}
		return sampleCount;
	}

	/**
	 * 返回这个 Chunk 的所有的 Sample.
	 * 
	 * @return 包含所有的 Sample 的列表.
	 */
	public List<Mp4Sample> getSamples() {
		if (samples == null) {
			return Collections.emptyList();
		}
		return samples;
	}

	/** 返回这个 Chunk 的大小. */
	public int getSize() {
		return size;
	}

	/** 指出这个 Chunk 是否为空. */
	public boolean isEmpty() {
		return (samples == null) || samples.isEmpty();
	}

	public void setChunkId(int chunkId) {
		this.chunkId = chunkId;
	}

	public void setChunkOffset(long chunkOffset) {
		this.chunkOffset = chunkOffset;
	}

	public void setPayload(int payload) {
		this.payload = payload;
	}

	public void setSampleCount(int sampleCount) {
		this.sampleCount = sampleCount;
	}
}
