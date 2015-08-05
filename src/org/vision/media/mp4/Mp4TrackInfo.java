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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaTypes;

/**
 * 代表一个 Track.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class Mp4TrackInfo extends MMediaFormat {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory
			.getLogger(Mp4TrackInfo.class);

	/** 这个 Track 包含的 Chunk. */
	private List<Mp4Chunk> chunks = new ArrayList<Mp4Chunk>();

	/** 当前 Chunk. */
	private Mp4Chunk currentChunk = new Mp4Chunk();

	private Mp4Sample currentSample;

	/** 每一个 Chunk 的 时间长度. */
	private long durationPerChunk;

	/** 这个 Track 的 Sample 的大小. */
	private long fixedSampleSize;

	/** 这个 Track 包含的 Sample 数 */
	private int sampleCount;

	private int sampleId;

	/** 每一个 Chunk 的 Sample 数. */
	private int samplesPerChunk;

	/** 这个 Track 的索引. */
	private int trackIndex = 0;

	public Mp4TrackInfo() {

	}

	/**
	 * 添加一个新的区块
	 * 
	 * @param chunkOffset
	 *            这个区块在文件中的开始位置
	 * @return
	 */
	public Mp4Chunk addChunk(long chunkOffset) {
		int chunkId = chunks.size() + 1;
		currentChunk.setChunkId(chunkId);
		currentChunk.setPayload(MMediaTypes.getMediaType(getMediaType()));
		currentChunk.setChunkOffset(chunkOffset);
		return currentChunk;
	}

	public void addChunk(Mp4Chunk chunk) {
		int chunkId = chunks.size() + 1;
		chunk.setChunkId(chunkId);
		chunks.add(chunk);
	}

	public long addSample(Mp4Sample sample) {
		long duration = getFixedSampleSize();
		if (sample.getSampleDuration() <= 0) {
			sample.setSampleDuration(duration);
		}

		sample.setSampleId(sampleCount + 1);

		currentChunk.addSample(sample);
		sampleCount++;

		// log.debug("addSample: " + sampleCount + ":" +
		// sample.getSampleTime());
		return 0;
	}

	public void clear() {
		chunks.clear();
		currentChunk.clear();
		sampleCount = 0;
	}

	public int getChunkCount() {
		return chunks.size();
	}

	public List<Mp4Chunk> getChunks() {
		return chunks;
	}

	public Mp4Chunk getCurrentChunk() {
		return currentChunk;
	}

	public Mp4Sample getCurrentSample() {
		return currentSample;
	}

	public long getDuration() {
		long duration = 0;
		for (Mp4Chunk chunk : chunks) {
			duration += chunk.getDuration();
		}

		int frequency = getSampleRate();
		if (frequency > 0 && frequency != 1000) {
			duration = duration * 1000 / frequency;
		}
		return duration;
	}

	/**
	 * @return the durationPerChunk
	 */
	public long getDurationPerChunk() {
		return durationPerChunk;
	}

	/**
	 * @return the fixedSampleSize
	 */
	public long getFixedSampleSize() {
		return fixedSampleSize;
	}

	public String getMetaString() {
		Mp4QueryString map = new Mp4QueryString();

		try {
			map.setAttribute("type", getMediaType());
			map.setAttribute("codecType", getCodecType());
			map.setAttribute("timescale", getSampleRate());

			if (getChannelCount() > 0) {
				map.setAttribute("channels", getChannelCount());
			}

			if (getVideoWidth() > 0) {
				map.setAttribute("width", getVideoWidth());
				map.setAttribute("height", getVideoHeight());
			}

			if (getFixedSampleSize() > 0) {
				map.setAttribute("sampleSize", getFixedSampleSize());
			}

		} catch (Exception e) {

		}

		return map.toQueryString();
	}

	public int getSampleCount() {
		return sampleCount;
	}

	public int getSampleId() {
		return sampleId;
	}

	public int getTrackIndex() {
		return trackIndex;
	}

	/**
	 * 指出当前 Chunk 是否已经满了.
	 * 
	 * @param sampleId
	 * @return
	 */
	protected boolean isChunkFull() {
		int sampleCount = currentChunk.getSampleCount();
		if ((samplesPerChunk > 0) && (sampleCount >= samplesPerChunk)) {
			return true;

		} else {
			return currentChunk.getDuration() >= durationPerChunk;
		}
	}

	public void setCurrentSample(Mp4Sample sample) {
		this.currentSample = sample;
	}

	public void setDurationPerChunk(long durationPerChunk) {
		this.durationPerChunk = durationPerChunk;
	}

	public void setFixedSampleSize(long fixedSampleSize) {
		this.fixedSampleSize = fixedSampleSize;
	}

	public void setSampleCountPerChunk(int samplesPerChunk) {
		this.samplesPerChunk = samplesPerChunk;
	}

	public void setSampleId(int sampleId) {
		this.sampleId = sampleId;
	}

	public void setTrackIndex(int trackIndex) {
		this.trackIndex = trackIndex;
	}

	@Override
	public String toString() {
		String info = "{";
		info += "type:" + getMediaType();
		info += ", codec:" + getCodecType();
		info += ", frequency:" + getSampleRate();
		info += ", channels:" + getChannelCount();
		info += ", width:" + getVideoWidth();
		info += ", height:" + getVideoHeight();
		info += ", duration:" + getDuration();
		info += ", sampleCount:" + getSampleCount();
		info += "}";
		return info;
	}
}
