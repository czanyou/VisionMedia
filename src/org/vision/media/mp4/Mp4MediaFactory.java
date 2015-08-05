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

import org.vision.media.MMediaMuxer;

public class Mp4MediaFactory {
	
	public static class MediaFrameInfo {
		public int videoWidth = 0;
		public int videoHeight = 0;
	}
	
	/**
	 * Creates a new MediaWriter to write audio/video samples into a file.
	 * 
	 * @param filename the file to write to
	 * @return a new MediaWriter
	 * @throws IOException
	 */
	public static MMediaMuxer openFile(String filename) throws IOException {
		return new Mp4Writer(filename);
	}
	
	public static MMediaMuxer openUnfinishedFile(String filename) throws IOException {
		Mp4Writer writer = new Mp4Writer();
		writer.openUnfinishedStream(filename);
		return writer;
	}

}
