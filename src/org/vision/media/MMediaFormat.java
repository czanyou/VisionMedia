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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代表一个媒体格式
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class MMediaFormat {

	/**
	 * A key describing the number of channels in an audio format. The
	 * associated value is an integer.
	 */
	public static final String KEY_CHANNEL_COUNT = "channel-count";

	public static final String KEY_CODEC_TYPE = "codec-type";

	/**
	 * A key describing the duration (in microseconds) of the content. The
	 * associated value is a long.
	 */
	public static final String KEY_DURATION = "durationUs";

	/**
	 * A key describing the frame rate of a video format in frames/sec. The
	 * associated value is an integer or a float.
	 */
	public static final String KEY_FRAME_RATE = "frame-rate";

	/**
	 * A key describing the height of the content in a video format. The
	 * associated value is an integer.
	 */
	public static final String KEY_HEIGHT = "height";

	public static final String KEY_MEDIA_TYPE = "media-type";

	/**
	 * A key describing the mime type of the MediaFormat. The associated value
	 * is a string.
	 */
	public static final String KEY_MIME = "mime";

	/**
	 * A key describing the sample rate of an audio format. The associated value
	 * is an integer.
	 */
	public static final String KEY_SAMPLE_RATE = "sample-rate";

	/**
	 * A key describing the width of the content in a video format. The
	 * associated value is an integer
	 */
	public static final String KEY_WIDTH = "width";

	public static MMediaFormat newAudioFormat(int codecType, int timeScale) {
		MMediaFormat mediaFormat = new MMediaFormat();
		mediaFormat.setSampleRate(timeScale);
		mediaFormat.setCodecType(codecType);
		mediaFormat.setVideoHeight(0);
		mediaFormat.setVideoWidth(0);
		mediaFormat.setMediaType("soun");

		return mediaFormat;
	}

	public static MMediaFormat newVideoFormat(int codecType, int width,
			int height) {
		MMediaFormat mediaFormat = new MMediaFormat();
		mediaFormat.setSampleRate(15000);
		mediaFormat.setCodecType(codecType);
		mediaFormat.setVideoHeight(height);
		mediaFormat.setVideoWidth(width);
		mediaFormat.setMediaType("video");

		return mediaFormat;
	}

	private static int parseInt(Object value, int defaultValue) {
		try {
			if (value == null) {
				return defaultValue;

			} else if (value instanceof Number) {
				return ((Number) value).intValue();

			} else if (value instanceof Boolean) {
				return ((Boolean) value) ? 1 : 0;

			} else {
				return Integer.parseInt(value.toString());
			}

		} catch (Exception e) {
			return defaultValue;
		}
	}

	private static long parseLong(Object value, long defaultValue) {
		try {
			if (value == null) {
				return defaultValue;

			} else if (value instanceof Number) {
				return ((Number) value).longValue();

			} else if (value instanceof Boolean) {
				return ((Boolean) value) ? 1 : 0;

			} else {
				return Long.parseLong(value.toString());
			}

		} catch (Exception e) {
			return defaultValue;
		}
	}

	/** The map containing all attributes. */
	private Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	public int getAttributeInt(String key) {
		return parseInt(attributes.get(key), 0);
	}

	public long getAttributeLong(String key) {
		return parseLong(attributes.get(key), 0);
	}

	public String getAttributeString(String key) {
		Object value = attributes.get(key);
		return (value != null) ? value.toString() : null;
	}

	/** 返回这个 Track 的音频通道数, 仅音频 Track 有效. */
	public int getChannelCount() {
		return getAttributeInt(KEY_CHANNEL_COUNT);
	}

	/** 返回这个 Track 的编码类型. */
	public int getCodecType() {
		return getAttributeInt(KEY_CODEC_TYPE);
	}

	/** 返回这个 Track 的媒体类型, 如 video, audio... */
	public String getMediaType() {
		return getAttributeString(KEY_MEDIA_TYPE);
	}

	/** 返回这个 Track 的 RTP 时间戳频率. */
	public int getSampleRate() {
		return getAttributeInt(KEY_SAMPLE_RATE);
	}

	/** 返回这个 Track 的 视频高度. */
	public int getVideoHeight() {
		return getAttributeInt(KEY_HEIGHT);
	}

	/** 返回这个 Track 的 视频宽度. */
	public int getVideoWidth() {
		return getAttributeInt(KEY_WIDTH);
	}

	public void setAttributeInt(String key, int value) {
		attributes.put(key, value);
	}

	public void setAttributeLong(String key, long value) {
		attributes.put(key, value);
	}

	public void setAttributeString(String key, String value) {
		attributes.put(key, value);
	}

	/** 设置这个 Track 的音频通道数, 仅音频 Track 有效. */
	public void setChannelCount(int channels) {
		setAttributeInt(KEY_CHANNEL_COUNT, channels);
	}

	/** 设置这个 Track 的编码类型. 如 H264, MPEG4-ES, AAC 等. */
	public void setCodecType(int codecType) {
		setAttributeInt(KEY_CODEC_TYPE, codecType);
	}

	/** 设置这个 Track 的媒体类型, 如 video, audio... */
	public void setMediaType(String mediaType) {
		setAttributeString(KEY_MEDIA_TYPE, mediaType);
	}

	/** 设置这个 Track 的 RTP 时间戳频率. */
	public void setSampleRate(int frequency) {
		setAttributeInt(KEY_SAMPLE_RATE, frequency);
	}

	public void setVideoHeight(int videoHeight) {
		setAttributeInt(KEY_HEIGHT, videoHeight);
	}

	public void setVideoWidth(int videoWidth) {
		setAttributeInt(KEY_WIDTH, videoWidth);
	}

}