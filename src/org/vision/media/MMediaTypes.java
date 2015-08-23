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

/**
 * 常用的媒体(音视频)编码类型.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class MMediaTypes {

	public static final int AAC = 2;

	/** Adaptive Multi-Rate narrowband audio codec, also known as AMR or AMR-NB. */
	public static final int AMR_NB = 0;

	/** Adaptive Multi-Rate narrowband audio codec, also known as AMR or AMR-NB. */
	public static final int AMR_WB = 1;

	public static final String AUDIO = "audio";

	public static final int AUDIO_TYPE = 1;

	/** h.264 视频编码 */
	public static final int H264 = 101;

	/** h.265 视频编码 */
	public static final int H265 = 107;

	/** G.711 a-law audio codec. */
	public static final int PCMA = 4;

	/** G.711 u-law audio codec. */
	public static final int PCMU = 5;

	public static final int TUNNEL_TYPE = 100;

	public static final String VIDEO = "video";

	public static final int VIDEO_TYPE = 0;

	public static int getMediaType(String type) {
		if (type == null) {
			return 999;

		} else if (VIDEO.equals(type)) {
			return VIDEO_TYPE;

		} else if (AUDIO.equals(type)) {
			return AUDIO_TYPE;
		}

		return 999;
	}
}
