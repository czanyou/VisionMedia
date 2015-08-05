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

/**
 * 代表一个 MP4 文件的 Track.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class Mp4Track {

	private int firstSampleId = 1;

	/** 这个 Track 的 Chunk 的第一个 Sample 表, (虚拟的表). */
	private Mp4TableProperty firstSampleTable;

	/** 固定的 Sample 长度. */
	private long fixedSampleSize;

	/** 这个 Track 的媒体持续时间, (mdia.mdhd.duration). */
	private Mp4Property mediaDuration;

	/** 这个 Track 的 Chunk 偏移位置表 (stco, Chunk offset atoms). */
	private Mp4TableProperty stcoTable;

	/** 这个 Track 的 Chunk 表 (stsc, Sample-to-chunk atoms). */
	private Mp4TableProperty stscTable;

	/** 这个 Track 的同步的 Sample 长度表 (stss, Sync sample atoms). */
	private Mp4TableProperty stssTable;

	/** 这个 Track 的 Sample 长度表 (stsz, Sample size atoms). */
	private Mp4TableProperty stszTable;

	/** 这个 Track 的 Sample 时间戳表 (stts, Time-to-sample atoms). */
	private Mp4TableProperty sttsTable;

	/** 这个 Track 的时间计算精度 (mdia.mdhd.timeScale). */
	private Mp4Property timeScale;

	/** 这个 Track 相关的 trak Atom 节点. */
	private Mp4Atom trackAtom;

	/** 这个 Track 的持续时间 (tkhd.duration). */
	private Mp4Property trackDuration;

	/** 这个 Track 的 Id (tkhd.trackId). */
	private Mp4Property trackId;

	/** 这个 Track 的类型 (mdia.hdlr.handler_type). */
	private Mp4Property trackType;

	/**
	 * 构建方法.
	 * 
	 * @param atom
	 *            相关的 Track ATOM.
	 */
	public Mp4Track() {
	}

	/**
	 * 添加一个 Chunk
	 * 
	 * @param sampleId
	 * @param chunkId
	 * @param sampleCount
	 * @param chunkOffset
	 */
	private void addChunk(int chunkId, int sampleCount, long chunkOffset) {
		addSampleToChunk(chunkId, sampleCount);
		addChunkOffset(chunkOffset);
	}

	/**
	 * 添加一个 Chunk
	 * 
	 * @param chunk
	 */
	public void addChunk(Mp4Chunk chunk) {
		for (Mp4Sample sample : chunk.getSamples()) {
			addSample(sample);
		}

		int chunkId = chunk.getChunkId();
		int sampleCount = chunk.getSampleCount();
		long chunkOffset = chunk.getChunkOffset();
		addChunk(chunkId, sampleCount, chunkOffset);
	}

	/**
	 * 添加/更新当前 Chunk 偏移位置
	 * 
	 * @param chunkOffset
	 */
	private void addChunkOffset(long chunkOffset) {
		if (stcoTable != null) {
			stcoTable.addRow(chunkOffset);
		}
	}

	private void addSample(int sampleId, long sampleSize, long duration,
			boolean isSyncPoint) {

		long scale = getTimeScale();
		duration = duration * scale * 10 / 1000000;

		// 4 舍 5 入
		duration = duration + 5;
		duration = duration / 10;

		// 第一个 sample 的时间长度通常为 0, 下一个 sample 的时间才是前一个 sample
		// 真正经过的时间, 所以跳过第一个 sample 传入的时间, 并在写完后, 要补一个
		// sample 的时间, 见 FinishWrite
		if (sampleId > 1) {
			addSampleTimes(duration);
		}

		addSampleSize(sampleId, sampleSize);

		if (isSyncPoint) {
			addSyncSample(sampleId);
		}

		updateMediaDuration(duration);
	}

	public void addSample(Mp4Sample sample) {
		addSample(sample.getSampleId(), sample.getSampleSize(),
				sample.getSampleDuration(), sample.isSyncPoint());
	}

	/**
	 * 更新当前样本的长度.
	 * 
	 * @param sampleId
	 *            样本的 ID
	 * @param sampleSize
	 *            样本的大小
	 */
	private void addSampleSize(long sampleId, long sampleSize) {
		if (stszTable != null) {
			stszTable.addRow(sampleSize);
		}
	}

	/**
	 * 
	 * @param duration
	 */
	private void addSampleTimes(long duration) {
		if (sttsTable == null) {
			return;
		}

		// if duration == duration of last entry
		long[] row = sttsTable.getLastRow();
		if ((row != null) && (duration == row[1])) {
			// increment last entry sampleCount
			row[0] = row[0] + 1;

		} else {
			// add stts entry, sampleCount = 1, sampleDuration = duration
			sttsTable.addRow(1, duration);
		}
	}

	/**
	 * 更新 Chunk 表格
	 * 
	 * @param sampleId
	 * @param chunkId
	 * @param sampleCount
	 */
	private void addSampleToChunk(int chunkId, int sampleCount) {
		if ((stscTable == null) || (firstSampleTable == null)) {
			return;
		}

		// if samplesPerChunk == samplesPerChunk of last entry
		int tableSize = stscTable.getRowCount();
		if (tableSize > 0) {
			long lastCount = stscTable.getValue(tableSize - 1, 1);
			if (lastCount == sampleCount) {
				return;
			}
		}

		// add stsc entry
		stscTable.addRow(chunkId, sampleCount, 1);
		firstSampleTable.addRow(firstSampleId);

		firstSampleId += sampleCount;
	}

	/**
	 * 添加一个同步 Sample
	 * 
	 * @param sampleId
	 * @param isSyncSample
	 */
	private void addSyncSample(int sampleId) {
		if (stssTable != null) {
			stssTable.addRow(sampleId);
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	public void finish() throws IOException {
		if (trackAtom == null) {
			return;
		}

		Mp4Atom stbl = trackAtom.findAtom("mdia.minf.stbl");
		if (stbl == null) {
			return;
		}

		if (stszTable != null) {
			if (stszTable.isEmpty()) {
				stbl.setProperty("stsz.sampleSize", fixedSampleSize);
				stbl.setProperty("stsz.entryCount", 0);

			} else {
				stbl.setProperty("stsz.sampleSize", 0);
				stbl.setProperty("stsz.entryCount", stszTable.getRowCount());
			}
		}

		if (stscTable != null) {
			stbl.setProperty("stsc.entryCount", stscTable.getRowCount());
		}

		if (stcoTable != null) {
			stbl.setProperty("stco.entryCount", stcoTable.getRowCount());
		}

		if (sttsTable != null) {
			stbl.setProperty("stts.entryCount", sttsTable.getRowCount());
		}

		if (stssTable != null) {
			stbl.setProperty("stss.entryCount", stssTable.getRowCount());
		}

		// mp4a
		Mp4Atom mp4a = stbl.findAtom("stsd.mp4a");
		if (mp4a != null) {
			mp4a.setProperty("esds.bufferSize", getMaxSampleSize());
			mp4a.setProperty("esds.avgBitrate", getAvgBitrate());
			mp4a.setProperty("esds.maxBitrate", getMaxBitrate());
		}
	}

	/** 计算当前 Track 的平均码率. */
	public int getAvgBitrate() {
		long duration = getDuration();
		if (duration == 0) {
			return -1;
		}

		double calc = getTotalOfSampleSizes();
		// this is a bit better - we use the whole duration
		calc *= 8.0;
		calc *= getTimeScale();
		calc /= duration;
		// we might want to think about rounding to the next 100 or 1000
		return (int) Math.ceil(calc);
	}

	/** 这个 Track 的 Chunk 的总共的数量. */
	public int getChunkCount() {
		return (stcoTable == null) ? 0 : stcoTable.getRowCount();
	}

	/** 返回这个 Track 的媒体持续时间. */
	public long getDuration() {
		return (mediaDuration == null) ? -1 : mediaDuration.getValueInt();
	}

	/** 计算当前 Track 的最大码率. */
	public int getMaxBitrate() {
		return (int) (getAvgBitrate() * 1.2f);
	}

	/**
	 * 返回最大的样本的长度.
	 * 
	 * @return 最大的样本的长度.
	 */
	public long getMaxSampleSize() {
		if ((stszTable == null) || stszTable.isEmpty()) {
			return fixedSampleSize;
		}

		long maxSampleSize = -1;
		long sampleCount = stszTable.getRowCount();
		for (int sid = 1; sid <= sampleCount; sid++) {
			long sampleSize = stszTable.getValue(sid - 1, 0);
			if (sampleSize > maxSampleSize) {
				maxSampleSize = sampleSize;
			}
		}
		return maxSampleSize;
	}

	/** 这个 Track 的 Sample 的总共的数量. */
	public int getSampleCount() {
		if (stszTable != null) {
			return stszTable.getRowCount();
		}

		return (sttsTable == null) ? 0 : sttsTable.getRowCount();
	}

	/**
	 * 返回指定的 Sample 的偏移位置.
	 * 
	 * @param sampleId
	 * @return
	 */
	public long getSampleOffset(long sampleId) {
		if ((stscTable == null) || (firstSampleTable == null)
				|| (stcoTable == null)) {
			return 0;
		}

		long stscIndex = getSampleStscIndex(sampleId);

		// firstChunk is the chunk index of the first chunk with
		// samplesPerChunk samples in the chunk. There may be multiples -
		// ie: several chunks with the same number of samples per chunk.
		long firstChunk = stscTable.getValue((int) stscIndex, 0);
		long firstSample = firstSampleTable.getValue((int) stscIndex, 0);
		long samplesPerChunk = stscTable.getValue((int) stscIndex, 1);
		if (samplesPerChunk <= 0) {
			return 0;
		}

		// chunkId tells which is the absolute chunk number that this sample
		// is stored in.
		long chunkId = firstChunk
				+ ((sampleId - firstSample) / samplesPerChunk);

		// chunkOffset is the file offset (absolute) for the start of the chunk
		long chunkOffset = stcoTable.getValue((int) (chunkId - 1), 0);
		long firstSampleInChunk = sampleId
				- ((sampleId - firstSample) % samplesPerChunk);

		// need cumulative samples sizes from firstSample to sampleId - 1
		long sampleOffset = 0;
		for (long i = firstSampleInChunk; i < sampleId; i++) {
			sampleOffset += getSampleSize(i);
		}

		return chunkOffset + sampleOffset;
	}

	/**
	 * 返回指定的 ID 的样本的长度, 如果不存在, 则返回 -1;
	 * 
	 * @param sampleId
	 *            样本的 ID
	 * @return 指定的 ID 的样本的长度
	 */
	public long getSampleSize(long sampleId) {
		if ((stszTable == null) || stszTable.isEmpty()) {
			return fixedSampleSize;

		} else {
			int index = (int) (sampleId - 1);
			return stszTable.getValue(index, 0);
		}
	}

	/**
	 * 返回指定的 Sample ID 在 STSC 表中的索引.
	 * 
	 * @param sampleId
	 * @return
	 */
	protected long getSampleStscIndex(long sampleId) {
		if (stscTable == null) {
			return 0xffffffff;
		}

		long stscIndex = 0;
		long numStscs = stscTable.getRowCount();
		if (numStscs == 0) {
			return 0xffffffff;
		}

		// TODO: 二分查找
		for (stscIndex = 0; stscIndex < numStscs; stscIndex++) {
			if (sampleId < firstSampleTable.getValue((int) stscIndex, 0)) {
				stscIndex -= 1;
				break;
			}
		}

		if (stscIndex == numStscs) {
			stscIndex -= 1;
		}

		return stscIndex;
	}

	/**
	 * 取得指定的 Sample 的时间戳等信息.
	 * 
	 * @param sample
	 */
	public void getSampleTimes(Mp4Sample sample) {
		if (sttsTable == null) {
			return;
		}

		long tableSize = sttsTable.getRowCount();
		long sampleId = 1;
		long elapsed = 0;

		int timeScale = getTimeScale();

		// TODO: 优化
		for (int i = 0; i < tableSize; i++) {
			long sampleCount = sttsTable.getValue(i, 0);
			long sampleDelta = sttsTable.getValue(i, 1);

			if ((timeScale > 0) && (timeScale != 1000000)) {
				sampleDelta = sampleDelta * 10000000 / timeScale;
				// 4 舍 5 入
				sampleDelta = sampleDelta + 5;
				sampleDelta = sampleDelta / 10;
			}

			if (sample.getSampleId() <= sampleId + sampleCount - 1) {
				long sampleTime = (sample.getSampleId() - sampleId);
				sampleTime *= sampleDelta;
				sampleTime += elapsed;

				sample.setSampleDuration(sampleDelta);
				sample.setSampleTime(sampleTime);
				return;
			}
			sampleId += sampleCount;
			elapsed += sampleCount * sampleDelta;
		}
	}

	private Mp4TableProperty getTableProperty(String name) {
		return (Mp4TableProperty) trackAtom.findProperty(name);
	}

	/** 返回这个 Track 的时间精度. */
	public int getTimeScale() {
		return (timeScale == null) ? 0 : (int) timeScale.getValueInt();
	}

	/**
	 * 计算所有的样本总共的大小.
	 * 
	 * @return
	 */
	protected double getTotalOfSampleSizes() {
		if ((stszTable == null) || stszTable.isEmpty()) {
			// if fixed sample size, just need to multiply by number of samples
			return fixedSampleSize * getSampleCount();
		}

		// else non-fixed sample size, sum them
		long totalSampleSizes = 0;
		long sampleCount = stszTable.getRowCount();
		for (int index = 0; index < sampleCount; index++) {
			totalSampleSizes += stszTable.getValue(index, 0);
		}
		return totalSampleSizes;
	}

	/** 这个 Track 相关的 trak Atom 节点. */
	public Mp4Atom getTrackAtom() {
		return trackAtom;
	}

	/** 这个 Track 的 Id. */
	public int getTrackId() {
		return (trackId == null) ? -1 : (int) trackId.getValueInt();
	}

	/** 这个 Track 的类型. */
	public String getType() {
		return (trackType == null) ? null : trackType.getValueString();
	}

	/** 这个 Track 的高度属性的值. */
	public int getVideoHeight() {
		return (int) trackAtom.getPropertyFloat("tkhd.height");
	}

	/** 这个 Track 的宽度属性的值. */
	public int getVideoWidth() {
		return (int) trackAtom.getPropertyFloat("tkhd.width");
	}

	/** 初始化这个 Track */
	public void init() {
		Mp4Atom track = trackAtom;
		if (track == null) {
			return;
		}

		// Atoms
		timeScale = track.findProperty("mdia.mdhd.timescale");
		trackId = track.findProperty("tkhd.track_ID");
		trackType = track.findProperty("mdia.hdlr.handler_type");
		trackDuration = track.findProperty("tkhd.duration");
		mediaDuration = track.findProperty("mdia.mdhd.duration");

		// Tables
		stszTable = getTableProperty("mdia.minf.stbl.stsz.entries");
		sttsTable = getTableProperty("mdia.minf.stbl.stts.entries");
		stscTable = getTableProperty("mdia.minf.stbl.stsc.entries");
		stcoTable = getTableProperty("mdia.minf.stbl.stco.entries");
		stssTable = getTableProperty("mdia.minf.stbl.stss.entries");

		// first sample table
		firstSampleTable = null;
		firstSampleId = 1;
		initFirstSampleTable();
	}

	/**
	 * 初始化一个 firstSamples 表格方便查找
	 */
	private void initFirstSampleTable() {
		if (stscTable == null) {
			return;
		}

		firstSampleTable = new Mp4TableProperty("firstSamples");
		int count = stscTable.getRowCount();
		int sampleId = 1;
		for (int i = 0; i < count; i++) {
			firstSampleTable.addRow(sampleId);
			if (i < (count - 1)) {
				long chunkId1 = stscTable.getValue(i + 1, 0);
				long chunkId0 = stscTable.getValue(i, 0);
				long sampleCount = stscTable.getValue(i, 1);
				sampleId += (chunkId1 - chunkId0) * sampleCount;
			}
		}
	}

	/**
	 * 指出指定的 ID 的 Sample 是否是同步的.
	 * 
	 * @param sampleId
	 * @return
	 */
	public boolean isSyncPoint(int sampleId) {
		if (stssTable == null) {
			return true; // 如果不存在 stss 则每一帧都是关键帧
		}

		// TODO: 二分查找
		int count = stssTable.getRowCount();
		for (int i = 0; i < count; i++) {
			long syncSampleId = stssTable.getValue(i, 0);
			if (sampleId == syncSampleId) {
				return true; // 如果这个 ID 包含在这个表中即表示它是一个关键帧

			} else if (sampleId < syncSampleId) {
				break;
			}
		}

		return false;
	}

	/** 设置这个 Track 的媒体持续时间. */
	public void setDuration(long duration) {
		if (mediaDuration != null) {
			mediaDuration.setValue(duration);
		}
	}

	/** 设置当前 Track 固定的样本大小. */
	public void setFixedSampleSize(long sampleSize) {
		// 如果样本大小表格不为空, 则 fixedSampleSize 没有意义
		fixedSampleSize = sampleSize;
	}

	/** 设置这个 Track 的时间精度. */
	public void setTimeScale(int scale) {
		if (timeScale != null) {
			timeScale.setValue(scale);
		}
	}

	/**  */
	public void setTrackAtom(Mp4Atom track) {
		if (track == null) {
			throw new NullPointerException("Null atom paremeter.");

		} else if (Mp4Atom.getAtomId("trak") != track.getType()) {
			throw new IllegalArgumentException("Not 'trak' aotm.");
		}

		trackAtom = track;
		init();
	}

	/** 这个 Track 的 Id. */
	public void setTrackId(int id) {
		if (trackId != null) {
			trackId.setValue(id);
		}
	}

	/** 这个 Track 的类型. */
	public void setType(String type) {
		if (trackType != null) {
			trackType.setValue(type);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("avgBitrate:" + getAvgBitrate());
		sb.append(", chunkCount:" + getChunkCount());
		sb.append(", maxBitrate:" + getMaxBitrate());
		sb.append(", maxSampleSize:" + getMaxSampleSize());
		sb.append(", sampleCount:" + getSampleCount());
		sb.append(", timeScale:" + getTimeScale());
		sb.append(", type:" + getType());
		sb.append(", totalSize:" + getTotalOfSampleSizes());
		sb.append("}");
		return sb.toString();
	}

	/**
	 * 更新相关的 Duration 的值.
	 * 
	 * @param duration
	 */
	private void updateMediaDuration(long duration) {
		// tack.mdia.mdhd.duration
		if (mediaDuration != null) {
			mediaDuration.setValue(mediaDuration.getValueInt() + duration);
		}
	}

	public void updateTrackDuration(long fileTimeScale) {
		long duration = getDuration();
		long timeScale = getTimeScale();
		if (timeScale > 0) {
			duration = (duration * fileTimeScale) / timeScale;
		}

		if (trackDuration != null) {
			trackDuration.setValue(duration);
		}
	}
}
