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
package org.vision.media.avc;

import org.vision.media.mp4.Mp4MediaFactory.MediaFrameInfo;

/**
 * MP4 视频流解析工具.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class Mp4Video {
	
	public static MediaFrameInfo parseSeqInfo(byte[] data) {
		MediaFrameInfo info = new MediaFrameInfo();
		Mp4Video.AvcSeqParams params = readSeqInfo(data);
		if (params != null) {
			info.videoWidth = params.pic_width;
			info.videoHeight = params.pic_height;
		}

		return info;
	}

	public static final int H264_NAL_TYPE_NON_IDR_SLICE = 0x01;
	public static final int H264_NAL_TYPE_DP_A_SLICE = 0x02;
	public static final int H264_NAL_TYPE_DP_B_SLICE = 0x03;
	public static final int H264_NAL_TYPE_DP_C_SLICE = 0x04;
	public static final int H264_NAL_TYPE_IDR_SLICE = 0x05;
	public static final int H264_NAL_TYPE_SEI = 0x06; //
	public static final int H264_NAL_TYPE_SEQ_PARAM = 0x07; // 序列参数集
	public static final int H264_NAL_TYPE_PIC_PARAM = 0x08; // 图像参数集
	public static final int H264_NAL_TYPE_ACCESS_UNIT = 0x09;
	public static final int H264_NAL_TYPE_END_OF_SEQ = 0x0a;
	public static final int H264_NAL_TYPE_END_OF_STREAM = 0x0b;
	public static final int H264_NAL_TYPE_FILLER_DATA = 0x0c;
	public static final int H264_NAL_TYPE_SEQ_EXTENSION = 0x0d;

	public static final int H264_TYPE_P = 0;
	public static final int H264_TYPE_B = 1;
	public static final int H264_TYPE_I = 2;
	public static final int H264_TYPE_SP = 3;
	public static final int H264_TYPE_SI = 4;
	public static final int H264_TYPE2_P = 5;
	public static final int H264_TYPE2_B = 6;
	public static final int H264_TYPE2_I = 7;
	public static final int H264_TYPE2_SP = 8;
	public static final int H264_TYPE2_SI = 9;

	public static class AvcSeqParams {
		public short profile;
		public short level;
		public int chroma_format_idc;
		public short residual_colour_transform_flag;
		public int bit_depth_luma_minus8;
		public int bit_depth_chroma_minus8;
		public short qpprime_y_zero_transform_bypass_flag;
		public short seq_scaling_matrix_present_flag;
		public int log2_max_frame_num_minus4;
		public int log2_max_pic_order_cnt_lsb_minus4;
		public int pic_order_cnt_type;
		public short pic_order_present_flag;
		public short delta_pic_order_always_zero_flag;
		public int offset_for_non_ref_pic;
		public int offset_for_top_to_bottom_field;
		public int pic_order_cnt_cycle_length;
		// public short offset_for_ref_frame[256];
		public int pic_width; // /< 视频的宽度
		public int pic_height; // /< 视频的高度
		public short frame_mbs_only_flag; // /< 是否只包括了帧
	}

	/**
	 * Bit 缓存区
	 * 
	 * @author ChengZhen(anyou@msn.com)
	 */
	public static class BitBuffer {
		/** 缓存的数据. */
		private byte[] data;

		/** 数据开始的位置. */
		private int pos;

		private int size;

		/** 比特缓存区. */
		private int bitsBuffer;

		/** 比特缓存区中比特的个数. */
		private int bitsInBuffer;

		private int bitsBufferMark;
		private int bitsInBufferMark;
		private int bufferSizeMark;
		private int bufferMark;

		private static final int masks[] = new int[] { 0x00000000, 0x00000001,
				0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f,
				0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff,
				0x00000fff, 0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff,
				0x0001ffff, 0x0003ffff, 0x0007ffff, 0x000fffff, 0x001fffff,
				0x003fffff, 0x007fffff, 0x00ffffff, 0x01ffffff, 0x03ffffff,
				0x07ffffff, 0x0fffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff,
				0xffffffff };

		public BitBuffer() {

		}

		/** 初始化缓存区. */
		public void init(byte[] bytes, int start) {
			data = bytes;
			pos = start;
			size = (bytes.length - start) * 8;
			bitsInBuffer = 0;
			bitsBuffer = 0;
		}

		/** 返回缓存区中还剩余的比特数. */
		public int remain() {
			return size + bitsInBuffer;
		}

		public int peekBits(int bits) {
			mark(true);
			int ret = getBits(bits);
			mark(false);
			return ret;
		}

		public int getBits(int bitsCount) {
			int ret = 0;
			if (bitsCount > 32) {
				throw new IndexOutOfBoundsException();
			} else if (bitsCount == 0) {
				return 0;
			}

			if (bitsInBuffer >= bitsCount) { // don't need to read from FILE
				bitsInBuffer -= bitsCount;
				ret = bitsBuffer >> bitsInBuffer;
				// wmay - this gets done below...ret &= msk[numBits];
			} else {
				int nbits = bitsCount - bitsInBuffer;
				if (nbits == 32) {
					ret = 0;
				} else {
					ret = bitsBuffer << nbits;
				}

				switch ((nbits - 1) / 8) {
				case 3:
					nbits -= 8;
					if (size < 8) {
						throw new IndexOutOfBoundsException();
					}
					ret |= data[pos++] << nbits;
					size -= 8;
					// fall through
				case 2:
					nbits -= 8;
					if (size < 8) {
						throw new IndexOutOfBoundsException();
					}
					ret |= data[pos++] << nbits;
					size -= 8;
				case 1:
					nbits -= 8;
					if (size < 8) {
						throw new IndexOutOfBoundsException();
					}
					ret |= data[pos++] << nbits;
					size -= 8;
				case 0:
					break;
				}

				if (size < nbits) {
					throw new IndexOutOfBoundsException();
				}
				bitsBuffer = data[pos++];
				bitsInBuffer = Math.min(8, size) - nbits;
				size -= Math.min(8, size);
				ret |= (bitsBuffer >> bitsInBuffer) & masks[nbits];
			}
			return (ret & masks[bitsCount]);
		}

		public void mark(boolean isSet) {
			if (isSet) {
				bitsInBufferMark = bitsInBuffer;
				bufferMark = pos;
				bufferSizeMark = size;
				bitsBufferMark = bitsBuffer;

			} else {
				bitsInBuffer = bitsInBufferMark;
				pos = bufferMark;
				size = bufferSizeMark;
				bitsBuffer = bitsBufferMark;
			}
		}
	}

	private static byte exp_golomb_bits[] = new byte[] { 8, 7, 6, 6, 5, 5, 5,
			5, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
			3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
			2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, };

	/** 指数哥伦布编码. 见 H.264 标准 9.1 节. */
	public static int h264_ue(BitBuffer bs) {
		int read = 0;
		int bits = 0;
		boolean done = false;

		// we want to read 8 bits at a time - if we don't have 8 bits,
		// read what's left, and shift. The exp_golomb_bits calc remains the
		// same.
		while (done == false) {
			int bits_left = bs.remain();
			if (bits_left < 8) {
				read = bs.peekBits(bits_left) << (8 - bits_left);
				done = true;
			} else {
				read = bs.peekBits(8);
				if (read == 0) {
					bs.getBits(8);
					bits += 8;
				} else {
					done = true;
				}
			}
		}

		byte coded = exp_golomb_bits[read];
		bs.getBits(coded);
		bits += coded;

		// printf("ue - bits %d\n", bits);
		return bs.getBits(bits + 1) - 1;
	}

	/** 有符号指数哥伦布编码. 见 H.264 标准 9.1.1 节. */
	public static int h264_se(BitBuffer bs) {
		int ret;
		ret = h264_ue(bs);
		if ((ret & 0x1) == 0) {
			ret >>= 1;
			int temp = 0 - ret;
			return temp;
		}
		return (ret + 1) >> 1;
	}

	/**
	 * 返回指定的 NALU 的类型.
	 * 
	 * @param offset
	 */
	public static int getNaluType(final byte[] buffer, int offset) {
		if ((buffer[offset] == 0x00) && (buffer[offset + 1] == 0x00)) {
			offset += (buffer[offset + 2] == 1) ? 3 : 4;
		}
		return buffer[offset] & 0x1f;
	}

	/** 返回指定找 NALU 的 Slice 类型. */
	public static int getSliceType(final byte[] buffer) {
		int header = 1;
		if ((buffer[0] == 0x00) && (buffer[1] == 0x00)) {
			header = (buffer[2] == 1) ? 4 : 5;
		}

		BitBuffer bs = new BitBuffer();
		bs.init(buffer, header);
		try {
			h264_ue(bs); // first_mb_in_slice
			int slice_type = h264_ue(bs); // slice type
			// h264_ue(bs); // pic_parameter_set_id ue(v) 指定使用的图像参数集
			// return bs.GetBits(0 + 4); // frame_num u(v) 用作一个图像标识符
			return slice_type;
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * 指出是否是同步点.
	 * 
	 * @param buffer
	 * @param offset
	 * @return
	 */
	public static boolean isSyncPoint(final byte[] buffer, int offset) {
		int type = getNaluType(buffer, offset);
		if (type == H264_NAL_TYPE_IDR_SLICE) {
			return true;

		} else if (type == H264_NAL_TYPE_SEQ_PARAM) {
			return true;
		}
		return false;
	}

	public static AvcSeqParams readSeqInfo(final byte[] buffer) {
		int header = 1;
		if ((buffer[0] == 0x00) && (buffer[1] == 0x00)) {
			header = (buffer[2] == 1) ? 4 : 5;
		}

		BitBuffer bs = new BitBuffer();
		bs.init(buffer, header);
		AvcSeqParams dec = new AvcSeqParams();
		try {
			dec.profile = (short) bs.getBits(8); // 是指比特流所遵守的配置和级别
			bs.getBits(1 + 1 + 1 + 1 + 4);
			dec.level = (short) bs.getBits(8); // 是指比特流所遵守的配置和级别

			h264_ue(bs); // seq_parameter_set_id // 用于识别图像参数集所指的序列参数集
			if (dec.profile == 100 || dec.profile == 110 || dec.profile == 122
					|| dec.profile == 144) {
				dec.chroma_format_idc = h264_ue(bs); // 与亮度取样对应的色度取样
				if (dec.chroma_format_idc == 3) {
					dec.residual_colour_transform_flag = (short) bs.getBits(1);
				}
				dec.bit_depth_luma_minus8 = h264_ue(bs); // 是指亮度队列样值的比特深度以及亮度量化参数范围的取值偏移
				dec.bit_depth_chroma_minus8 = h264_ue(bs); // 是指色度队列样值的比特深度以及色度量化参数范围的取值偏移
				dec.qpprime_y_zero_transform_bypass_flag = (short) bs
						.getBits(1);
				dec.seq_scaling_matrix_present_flag = (short) bs.getBits(1);
				if (dec.seq_scaling_matrix_present_flag > 0) {
					for (int ix = 0; ix < 8; ix++) {
						if (bs.getBits(1) > 0) {
							scaling_list(ix < 6 ? 16 : 64, bs);
						}
					}
				}
			}

			dec.log2_max_frame_num_minus4 = h264_ue(bs);
			dec.pic_order_cnt_type = h264_ue(bs); // 是指解码图像顺序的计数方法
			if (dec.pic_order_cnt_type == 0) {
				dec.log2_max_pic_order_cnt_lsb_minus4 = h264_ue(bs);

			} else if (dec.pic_order_cnt_type == 1) {
				dec.delta_pic_order_always_zero_flag = (short) bs.getBits(1);
				dec.offset_for_non_ref_pic = h264_se(bs); // offset_for_non_ref_pic
				dec.offset_for_top_to_bottom_field = h264_se(bs); // offset_for_top_to_bottom_field
				dec.pic_order_cnt_cycle_length = h264_ue(bs); // poc_cycle_length
				for (int ix = 0; ix < dec.pic_order_cnt_cycle_length; ix++) {
					/* dec.offset_for_ref_frame[MIN(ix,255)] = */h264_se(bs); // offset
																				// for
																				// ref
																				// fram
																				// -
				}
			}

			h264_ue(bs); // num_ref_frames
			bs.getBits(1); // gaps_in_frame_num_value_allowed_flag
			int PicWidthInMbs = h264_ue(bs) + 1; // 加 1 是指以宏块为单元的每个解码图像的宽度。
			dec.pic_width = PicWidthInMbs * 16;
			int PicHeightInMapUnits = h264_ue(bs) + 1; // 加 1
														// 表示以条带组映射为单位的一个解码帧或场的高度。

			dec.frame_mbs_only_flag = (short) bs.getBits(1); // 等于 0
																// 表示编码视频序列的编码图像可能是编码场或编码帧
			dec.pic_height = (2 - dec.frame_mbs_only_flag)
					* PicHeightInMapUnits * 16;
			return dec;

		} catch (Exception e) {
			return null;
		}
	}

	private static void scaling_list(int sizeOfScalingList, BitBuffer bs) {
		int lastScale = 8, nextScale = 8;
		int j;

		for (j = 0; j < sizeOfScalingList; j++) {
			if (nextScale != 0) {
				int deltaScale = h264_se(bs);
				nextScale = (lastScale + deltaScale + 256) % 256;
			}
			if (nextScale == 0) {
				nextScale = lastScale;
			} else {
				lastScale = nextScale;
			}
		}
	}

}
