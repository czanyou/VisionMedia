package org.vision.media.hls;

public interface MpegTS {
	public static final int MAX_PES_PACKET_SIZE = 1280 * 720 * 3;
	public static final int STREAM_TYPE_AUDIO_AAC = 0x0F;
	public static final int STREAM_TYPE_VIDEO_H264 = 0x1B;

	public static final int STREAM_TYPE_VIDEO_MPEG4 = 0x10;
	public static final int TS_AUDIO_PID = 0x0101;
	public static final int TS_PACKET_SIZE = 188;
	public static final int TS_PAT_PID = 0x0000;
	public static final int TS_PAT_TABLE_ID = 0x00;

	public static final int TS_PAYLOAD_SIZE = 184;
	public static final int TS_PCR_PID = 0x0100;
	public static final int TS_PMT_PID = 0x0FFF;

	public static final int TS_PMT_TABLE_ID = 0x02;
	public static final int TS_PTS_BASE = 63000;
	public static final int TS_START_CODE = 0x47;

	public static final int TS_VIDEO_PID = 0x0100;
}
