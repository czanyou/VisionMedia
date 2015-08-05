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

import org.vision.media.mp4.Mp4MediaFactory;

/**
 * 代表一个媒体 Wrtier
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public abstract class MMediaMuxer {
	
	/** MPEG4 media file format. */
	public static final int MUXER_OUTPUT_MPEG_4 = 0;
	
	public static final int MUXER_OUTPUT_MPEG_TS = 1;
	
	/**
	 * Creates a new MediaWriter to write audio/video samples into a file.
	 * 
	 * @param filename
	 *            the file to write to
	 * @return a new MediaWriter
	 * @throws IOException
	 */
	public static MMediaMuxer openFile(String filename, int format) throws IOException {
		return Mp4MediaFactory.openFile(filename);
	}

	/**
	 * 指定音频的格式.
	 * 
	 * @param mediaFormat
	 * @return
	 */
	public abstract int setAudioFormat(MMediaFormat mediaFormat);

	/**
	 * 指定视频的格式.
	 * 
	 * @param mediaFormat
	 * @return
	 */
	public abstract int setVideoFormat(MMediaFormat mediaFormat);

	/**
	 * 准备开始写 MP4 文件. 这个方法会先将 mdat 之前的 ATOM 节点和 mdat 节点的 头部先写入文件, 这样接着就可以开始写
	 * sample 数据了, 而 mdat 之后的节点 要等 mdat 写完之后, 才会在调用 finish 时再写入文件中.
	 * 
	 * Make sure this is called after addTrack(MediaFormat) and before
	 * writeSampleData(int, ByteBuffer, MediaCodec.BufferInfo).
	 * 
	 * @throws IOException
	 *             如果发生 I/O 错误。
	 * @throws IllegalStateException
	 *             如果当前的状态不是 INIT.
	 */
	public abstract void start() throws IOException;

	/**
	 * 关闭这个书写器. Once the muxer stops, it can not be restarted.
	 */
	public abstract void stop();

	/**
	 * 向文件中写入指定的音频 Sample.
	 * 
	 * @throws IOException
	 *             如果发生 I/O 错误。
	 * @throws IllegalStateException
	 *             如果当前的状态不是 STARTED.
	 */
	public abstract long writeAudioData(MMediaBuffer mediaBuffer);

	/**
	 * 向文件中写入指定的视频 Sample.
	 * 
	 * Writes an encoded sample into the muxer.
	 * <p>
	 * The application needs to make sure that the samples are written into the
	 * right tracks. Also, it needs to make sure the samples for each track are
	 * written in chronological order (e.g. in the order they are provided by
	 * the encoder.)
	 * </p>
	 * 
	 * @throws IOException
	 *             如果发生 I/O 错误。
	 * @throws IllegalStateException
	 *             如果当前的状态不是 STARTED.
	 */

	public abstract long writeVideoData(MMediaBuffer mediaBuffer);

}