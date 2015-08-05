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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.vision.media.mp4.Mp4Reader;

/**
 * 代表一个媒体流 Reader 接口.
 * 
 * MediaExtractor facilitates extraction of demuxed, typically encoded, media
 * data from a data source.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public abstract class MMediaExtractor {
	
	/** The sample is a sync sample */
	public static final int FLAG_SYNC_SAMPLE = 0x01;

	public static MMediaExtractor openFile(String filename) throws IOException {
		MMediaExtractor reader = new Mp4Reader();
		reader.setDataSource(filename);
		return reader;
	}

	/** Advance to the next sample. */
	public abstract boolean advance();

	/**
	 * Make sure you call this when you're done to free up any resources instead
	 * of relying on the garbage collector to do this for you at some point in
	 * the future.
	 */
	public abstract void close();

	/**
	 * Returns an estimate of how much data is presently cached in memory
	 * expressed in microseconds.
	 */
	public abstract long getCachedDuration();

	/**
	 * 返回总共的长度
	 * 
	 * @return
	 */
	public abstract long getDuration();

	/**
	 * 返回当前的位置.
	 * 
	 * @return
	 */
	public abstract long getPosition();

	/** Returns the current sample's flags. */
	public abstract int getSampleFlags();

	/** Returns the current sample's presentation time in microseconds. */
	public abstract long getSampleTime();

	/**
	 * Returns the track index the current sample originates from (or -1 if no
	 * more samples are available)
	 */
	public abstract int getSampleTrackIndex();

	/**
	 * Count the number of tracks found in the data source.
	 * 
	 * @return
	 */
	public abstract int getTrackCount();

	/**
	 * Get the track format at the specified index.
	 * 
	 * @param index
	 */
	public abstract MMediaFormat getTrackFormat(int index);

	/**
	 * Returns true iff we are caching data and the cache has reached the end of
	 * the data stream (for now, a future seek may of course restart the
	 * fetching of data).
	 */
	public abstract boolean hasCacheReachedEndOfStream();

	/**
	 * Retrieve the current encoded sample and store it in the byte buffer
	 * starting at the given offset.
	 * 
	 * @param byteBuffer
	 * @param offset
	 * @return Returns the sample size (or -1 if no more samples are available).
	 */
	public abstract int readSampleData(ByteBuffer byteBuffer, int offset);

	/**
	 * 定位到指定的时间.
	 * 
	 * All selected tracks seek near the requested time according to the
	 * specified mode.
	 * 
	 * @param time
	 */
	public abstract void seekTo(long time, int flags);

	/**
	 * Subsequent calls to readSampleData(ByteBuffer, int),
	 * getSampleTrackIndex() and getSampleTime() only retrieve information for
	 * the subset of tracks selected.
	 * 
	 * @param index
	 */
	public abstract void selectTrack(int index);

	/**
	 * Sets the data source (file-path or http URL) to use.
	 * 
	 * @param path
	 */
	public abstract void setDataSource(String path) throws IOException;

	/**
	 * Subsequent calls to readSampleData(ByteBuffer, int),
	 * getSampleTrackIndex() and getSampleTime() only retrieve information for
	 * the subset of tracks selected.
	 * 
	 * @param index
	 */
	public abstract void unselectTrack(int index);

}