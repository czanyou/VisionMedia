package org.vision.media.mp4;

public class Mp4Common {
	public static final int MOV_TRUN_DATA_OFFSET = 0x01;
	public static final int MOV_TRUN_FIRST_SAMPLE_FLAGS = 0x04;
	public static final int MOV_TRUN_SAMPLE_DURATION = 0x100;
	public static final int MOV_TRUN_SAMPLE_SIZE = 0x200;
	public static final int MOV_TRUN_SAMPLE_FLAGS = 0x400;
	public static final int MOV_TRUN_SAMPLE_CTS = 0x800;

	public static final int MOV_TFHD_BASE_DATA_OFFSET = 0x01;
	public static final int MOV_TFHD_STSD_ID = 0x02;
	public static final int MOV_TFHD_DEFAULT_DURATION = 0x08;
	public static final int MOV_TFHD_DEFAULT_SIZE = 0x10;
	public static final int MOV_TFHD_DEFAULT_FLAGS = 0x20;
	public static final int MOV_TFHD_DURATION_IS_EMPTY = 0x010000;

	public static byte[] START_CODE = new byte[] { 0x00, 0x00, 0x00, 0x01 };
}
