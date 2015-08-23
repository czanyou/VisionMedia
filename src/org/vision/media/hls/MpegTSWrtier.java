package org.vision.media.hls;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaMuxer;
import org.vision.media.mp4.Mp4IndexWriter;

public class MpegTSWrtier extends MMediaMuxer implements MpegTS {
	public static final long[] crc_table = new long[] { 0x00000000, 0x04c11db7,
			0x09823b6e, 0x0d4326d9, 0x130476dc, 0x17c56b6b, 0x1a864db2,
			0x1e475005, 0x2608edb8, 0x22c9f00f, 0x2f8ad6d6, 0x2b4bcb61,
			0x350c9b64, 0x31cd86d3, 0x3c8ea00a, 0x384fbdbd, 0x4c11db70,
			0x48d0c6c7, 0x4593e01e, 0x4152fda9, 0x5f15adac, 0x5bd4b01b,
			0x569796c2, 0x52568b75, 0x6a1936c8, 0x6ed82b7f, 0x639b0da6,
			0x675a1011, 0x791d4014, 0x7ddc5da3, 0x709f7b7a, 0x745e66cd,
			0x9823b6e0, 0x9ce2ab57, 0x91a18d8e, 0x95609039, 0x8b27c03c,
			0x8fe6dd8b, 0x82a5fb52, 0x8664e6e5, 0xbe2b5b58, 0xbaea46ef,
			0xb7a96036, 0xb3687d81, 0xad2f2d84, 0xa9ee3033, 0xa4ad16ea,
			0xa06c0b5d, 0xd4326d90, 0xd0f37027, 0xddb056fe, 0xd9714b49,
			0xc7361b4c, 0xc3f706fb, 0xceb42022, 0xca753d95, 0xf23a8028,
			0xf6fb9d9f, 0xfbb8bb46, 0xff79a6f1, 0xe13ef6f4, 0xe5ffeb43,
			0xe8bccd9a, 0xec7dd02d, 0x34867077, 0x30476dc0, 0x3d044b19,
			0x39c556ae, 0x278206ab, 0x23431b1c, 0x2e003dc5, 0x2ac12072,
			0x128e9dcf, 0x164f8078, 0x1b0ca6a1, 0x1fcdbb16, 0x018aeb13,
			0x054bf6a4, 0x0808d07d, 0x0cc9cdca, 0x7897ab07, 0x7c56b6b0,
			0x71159069, 0x75d48dde, 0x6b93dddb, 0x6f52c06c, 0x6211e6b5,
			0x66d0fb02, 0x5e9f46bf, 0x5a5e5b08, 0x571d7dd1, 0x53dc6066,
			0x4d9b3063, 0x495a2dd4, 0x44190b0d, 0x40d816ba, 0xaca5c697,
			0xa864db20, 0xa527fdf9, 0xa1e6e04e, 0xbfa1b04b, 0xbb60adfc,
			0xb6238b25, 0xb2e29692, 0x8aad2b2f, 0x8e6c3698, 0x832f1041,
			0x87ee0df6, 0x99a95df3, 0x9d684044, 0x902b669d, 0x94ea7b2a,
			0xe0b41de7, 0xe4750050, 0xe9362689, 0xedf73b3e, 0xf3b06b3b,
			0xf771768c, 0xfa325055, 0xfef34de2, 0xc6bcf05f, 0xc27dede8,
			0xcf3ecb31, 0xcbffd686, 0xd5b88683, 0xd1799b34, 0xdc3abded,
			0xd8fba05a, 0x690ce0ee, 0x6dcdfd59, 0x608edb80, 0x644fc637,
			0x7a089632, 0x7ec98b85, 0x738aad5c, 0x774bb0eb, 0x4f040d56,
			0x4bc510e1, 0x46863638, 0x42472b8f, 0x5c007b8a, 0x58c1663d,
			0x558240e4, 0x51435d53, 0x251d3b9e, 0x21dc2629, 0x2c9f00f0,
			0x285e1d47, 0x36194d42, 0x32d850f5, 0x3f9b762c, 0x3b5a6b9b,
			0x0315d626, 0x07d4cb91, 0x0a97ed48, 0x0e56f0ff, 0x1011a0fa,
			0x14d0bd4d, 0x19939b94, 0x1d528623, 0xf12f560e, 0xf5ee4bb9,
			0xf8ad6d60, 0xfc6c70d7, 0xe22b20d2, 0xe6ea3d65, 0xeba91bbc,
			0xef68060b, 0xd727bbb6, 0xd3e6a601, 0xdea580d8, 0xda649d6f,
			0xc423cd6a, 0xc0e2d0dd, 0xcda1f604, 0xc960ebb3, 0xbd3e8d7e,
			0xb9ff90c9, 0xb4bcb610, 0xb07daba7, 0xae3afba2, 0xaafbe615,
			0xa7b8c0cc, 0xa379dd7b, 0x9b3660c6, 0x9ff77d71, 0x92b45ba8,
			0x9675461f, 0x8832161a, 0x8cf30bad, 0x81b02d74, 0x857130c3,
			0x5d8a9099, 0x594b8d2e, 0x5408abf7, 0x50c9b640, 0x4e8ee645,
			0x4a4ffbf2, 0x470cdd2b, 0x43cdc09c, 0x7b827d21, 0x7f436096,
			0x7200464f, 0x76c15bf8, 0x68860bfd, 0x6c47164a, 0x61043093,
			0x65c52d24, 0x119b4be9, 0x155a565e, 0x18197087, 0x1cd86d30,
			0x029f3d35, 0x065e2082, 0x0b1d065b, 0x0fdc1bec, 0x3793a651,
			0x3352bbe6, 0x3e119d3f, 0x3ad08088, 0x2497d08d, 0x2056cd3a,
			0x2d15ebe3, 0x29d4f654, 0xc5a92679, 0xc1683bce, 0xcc2b1d17,
			0xc8ea00a0, 0xd6ad50a5, 0xd26c4d12, 0xdf2f6bcb, 0xdbee767c,
			0xe3a1cbc1, 0xe760d676, 0xea23f0af, 0xeee2ed18, 0xf0a5bd1d,
			0xf464a0aa, 0xf9278673, 0xfde69bc4, 0x89b8fd09, 0x8d79e0be,
			0x803ac667, 0x84fbdbd0, 0x9abc8bd5, 0x9e7d9662, 0x933eb0bb,
			0x97ffad0c, 0xafb010b1, 0xab710d06, 0xa6322bdf, 0xa2f33668,
			0xbcb4666d, 0xb8757bda, 0xb5365d03, 0xb1f740b4 };

	protected static final Logger log = LoggerFactory
			.getLogger(Mp4IndexWriter.class);

	private int fContinuityCounter;

	private String fFileName;
	private byte[] fFrameBuffer;
	private int fFrameSize;
	private boolean fIsStop;
	private OutputStream fOutputStream;
	private int fPacketIndex;
	private int fPATContinuityCounter;
	private int fPMTContinuityCounter;

	public MpegTSWrtier(String filename) {
		fFileName = filename;
	}

	@Override
	public void start() throws IOException {
		
	}

	private OutputStream getOutputStream() {
		try {
			return new FileOutputStream(fFileName);
		} catch (FileNotFoundException e) {
			log.debug(e.getMessage(), e);
			return null;
		}
	}

	private long mpegts_crc32(byte[] data, int offset, int len) {
		long crc = 0xffffffff;
		for (int i = 0; i < len; i++) {
			int pos = (int) ((crc >> 24) ^ data[i + offset]) & 0xff;
			crc = (crc << 8) ^ crc_table[pos];
		}

		return crc;
	}

	public void stop() {
		fPacketIndex = 0;

		if (fOutputStream != null) {
			try {
				fOutputStream.flush();
				fOutputStream.close();
				fOutputStream = null;

			} catch (IOException e) {
				log.debug(e.getMessage(), e);
			}
		}
	}

	@Override
	public int setAudioFormat(MMediaFormat mediaFormat) {
		return 0;
	}

	public void setFilename(String filename) {
		fFileName = filename;
	}

	public void setOutputStream(OutputStream out) {
		fOutputStream = out;
	}

	@Override
	public int setVideoFormat(MMediaFormat mediaFormat) {
		return 0;
	}

	@Override
	public long writeAudioData(MMediaBuffer mediaBuffer) {
		return 0;
	}

	/** 写入指定的 ES 包. */
	private int writeESPacket(ByteBuffer data, int length, long pts,
			boolean isEnd) {
		if (fFrameBuffer == null) {
			fFrameBuffer = new byte[MAX_PES_PACKET_SIZE];
		}

		// 无效的帧
		if (fFrameSize + length > MAX_PES_PACKET_SIZE) {
			fFrameSize = 0;
			return 0;
		}

		if (fFrameSize == 0) {
			int PESHeaderLength = writePESHeader(fFrameBuffer, pts);
			fFrameSize += PESHeaderLength;

			byte au[] = { 0x00, 0x00, 0x00, 0x01, 0x09, 0x10, 0x00 };
			System.arraycopy(au, 0, fFrameBuffer, fFrameSize, 6);
			fFrameSize += 6;

			data.get(fFrameBuffer, fFrameSize, length);
			fFrameSize += length;

		} else {
			data.get(fFrameBuffer, fFrameSize, length);
			fFrameSize += length;
		}

		if (isEnd) {
			writePESPacket(fFrameBuffer, fFrameSize, pts);

			fFrameSize = 0;
		}
		return 0;
	}

	/** 生成 PAT 包. */
	private int writePATPacket() {
		byte[] buffer = new byte[TS_PACKET_SIZE];
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) 0xff;
		}

		long PMT_ID = TS_PMT_PID;
		int PAT_TABLE_OFFSET = 5;
		byte[] p = buffer;
		int tableLength = 13; // 13
		int i = 0;

		// TS Packet Header (4 Bytes)
		p[i++] = TS_START_CODE; // 8: 同步字节, 为 0x47
		p[i++] = 0x40; // (0100 0000) 1: 传输误码指示符, 1: 起始指示符, 1: 优先传输
		p[i++] = 0x00; // 13: PID
		p[i++] = (byte) (0x10 | fPATContinuityCounter); // 2: 传输加扰, 2: 自适应控制 4:
														// 连续计数器
		fPATContinuityCounter = (fPATContinuityCounter + 1) & 0x0F;

		// 0x00
		p[i++] = 0x00;

		// PAT Table
		p[i++] = TS_PAT_TABLE_ID; // 8: 固定为0x00, 标志是该表是PAT
		p[i++] = (byte) (0xB0 | ((tableLength >> 8) & 0x0F)); // 1: 段语法标志位，固定为1;
																// 1: 0; 2: 保留
																// (1011 0000)
		p[i++] = (byte) (tableLength & 0xff); // 12: 13, 表示这个字节后面有用的字节数，包括CRC32

		p[i++] = 0x00; //
		p[i++] = 0x01; // 16: 该传输流的ID，区别于一个网络中其它多路复用的流
		p[i++] = (byte) (0xC1); // 2: 保留; 5: 范围0-31，表示PAT的版本号; 1:
								// 发送的PAT是当前有效还是下一个PAT有效 (1100 0001)
		p[i++] = 0x00; // 8: 分段的号码。PAT可能分为多段传输，第一段为00，以后每个分段加1，最多可能有256个分段
		p[i++] = 0x00; // 8: 最后一个分段的号码

		// Programs 节目列表
		p[i++] = 0x00; //
		p[i++] = 0x01; // 16: 节目号
		p[i++] = (byte) (0xE0 | ((PMT_ID >> 8) & 0x1F)); // 3: 保留位 (1110 0001)
		p[i++] = (byte) (PMT_ID & 0xff); // 13: 节目映射表的PID，节目号大于 0 时对应的
											// PID，每个节目对应一个
		// 13: 网络信息表(NIT)的 PID,节目号为 0 时对应的 PID 为network_PID

		// CRC 32
		long crc = mpegts_crc32(buffer, PAT_TABLE_OFFSET, (tableLength + 3) - 4);
		p[i++] = (byte) ((crc >> 24) & 0xff);
		p[i++] = (byte) ((crc >> 16) & 0xff);
		p[i++] = (byte) ((crc >> 8) & 0xff);
		p[i++] = (byte) ((crc) & 0xff);

		writeTSPacket(buffer, TS_PACKET_SIZE);
		return 0;
	}

	/** 写入指定的 PCR 节目时钟基准 (Program clock reference). */
	private int writePCR(byte[] buffer, int offset, long pcr) {
		byte[] p = buffer;
		if (p == null) {
			return -1;
		}

		int i = offset;

		// (33bit) program clock reference base
		p[i++] = (byte) ((pcr >> 25) & 0xff); //
		p[i++] = (byte) ((pcr >> 17) & 0xff); //
		p[i++] = (byte) ((pcr >> 9) & 0xff); //
		p[i++] = (byte) ((pcr >> 1) & 0xff); //

		// p[i++] = ((pcr & 0x01) << 7) | 0x7E; //(6bit) reserved
		p[i++] = 0x00;

		// (9bit) Program clock reference extension
		p[i++] = 0x00; //

		return 0;
	}

	/** 写入指定的 PES 头信息. */
	private int writePESHeader(byte[] buffer, long pts) {
		long dts = pts;
		int streamID = 0xE0;
		byte[] pes = buffer;
		int i = 0;

		// 3 Byte: 0x000001 包起始码前缀
		// 1 Byte: 0xE0 数据流识别码
		// 2 Byte: PES 包长度
		// 2 Byte: PES 包头识别标志
		// 2: ID, 0x20
		// 2: PES 加扰控制
		// 1: PES 优先
		// 1: 数据定位指示符
		// 1: 版权
		// 1: 原版或拷贝

		// 2: PTS/DTS 标志
		// 1: ESCR 标志
		// 1: 基本速率标志
		// 1: DSM
		// 1: 附加信息
		// 1: PES CRC 标志
		// 1: PES 扩展标志
		// 1 Byte: PES 包头长度
		// X Byte:

		// PTS: 显示时间戳, DTS 解码时间戳, DSM 数据存储媒体, ESCE 基本流时钟基准

		// Start Code
		pes[i++] = 0x00;
		pes[i++] = 0x00;
		pes[i++] = 0x01;

		// Stream ID
		pes[i++] = (byte) streamID; // Stream ID, E0: Video; C0: Audio

		// PES Length
		pes[i++] = 0x00; //
		pes[i++] = 0x00; // 16:

		// Flags
		pes[i++] = (byte) 0x84; // (1000 0100) data_alignment
		// 2: mpeg2 id, 0x20
		// 2: pes scrambling control
		// 1: pes priority
		// 1: data alignement indicator
		// 1: copyright
		// 1: original or copy

		pes[i++] = (byte) 0xC0; // (1100 0000) PTS DTS
		// 2: pts_dts flags
		// 1: escr flags
		// 1: es rate flag
		// 1: dsm trick mode flag
		// 1: additional copy info flag
		// 1: pes crc flag
		// 1: pes extention flags

		pes[i++] = 0x0A; // 8: Data Length

		// PTS 5 Byte
		pes[i++] = (byte) (((pts >> 30) & 0xFE) | 0x31); // 4: '0010' or '0011',
															// 3: PTS, 1: marker
		pes[i++] = (byte) ((pts >> 22) & 0xff); // 15: PTS
		pes[i++] = (byte) (((pts >> 14) & 0xFE) | 0x01); // 1: marker
		pes[i++] = (byte) ((pts >> 7) & 0xff); // 15: PTS
		pes[i++] = (byte) ((pts << 1) & 0xFE | 0x01); // 1: marker

		// DTS 5 Byte
		pes[i++] = (byte) (((dts >> 29) & 0xFE) | 0x11); // 4: '0010' or '0011',
															// 3: PTS, 1: marker
		pes[i++] = (byte) ((dts >> 22) & 0xff); // 15: PTS
		pes[i++] = (byte) (((dts >> 14) & 0xFE) | 0x01); // 1: marker
		pes[i++] = (byte) ((dts >> 7) & 0xff); // 15: PTS
		pes[i++] = (byte) ((dts << 1) & 0xFE | 0x01); // 1: marker

		return 9 + 10;
	}

	/** 写入指定的 PES 包. */
	private int writePESPacket(byte[] data, int length, long pts) {
		byte buffer[] = new byte[256];
		boolean isStart = true;
		int pid = TS_VIDEO_PID;
		long pcr = pts;

		int leftover = length;
		int dataOffset = 0;
		while (leftover > 0) {
			// BOOL isAdaptationField = (isStart || (leftover <
			// TS_PAYLOAD_SIZE)) ? 1 : 0;
			boolean isAdaptationField = (leftover < TS_PAYLOAD_SIZE) ? true
					: false;

			// TS Packet Header
			buffer[0] = TS_START_CODE;
			buffer[1] = (byte) ((isStart ? 0x40 : 0x00) | ((pid >> 8) & 0x1f));
			buffer[2] = (byte) (pid & 0xff);
			buffer[3] = (byte) ((isAdaptationField ? 0x30 : 0x10) | fContinuityCounter);
			fContinuityCounter = (fContinuityCounter + 1) & 0x0F;

			long size = 0;
			if (leftover < TS_PAYLOAD_SIZE) {
				size = leftover;
				long stuffing = TS_PAYLOAD_SIZE - size;

				if (stuffing > 0) {
					buffer[4] = (byte) (stuffing - 1); // 长度
				}

				if (stuffing > 1) {
					buffer[5] = 0x00; // 总是为 0x00
				}

				if (stuffing > 2) {
					for (int i = 0; i < stuffing - 2; i++) {
						buffer[i + 6] = (byte) 0xff;
					}
				}

				if (stuffing > 8) {
					buffer[5] |= 0x10; // flags: PCR present
					writePCR(buffer, 6, pcr); // 6 Bytes
				}

				System.arraycopy(data, dataOffset, buffer,
						(int) (4 + stuffing), (int) size);

			} else {
				size = TS_PAYLOAD_SIZE;
				System.arraycopy(data, dataOffset, buffer, 4, (int) size);
			}

			writeTSPacket(buffer, TS_PACKET_SIZE);

			isStart = false;
			dataOffset += size;
			leftover -= size;
		}

		return 0;
	}

	/** 生成 PMT 包, 这个包用来描述指定的节目的编码格式等信息. */
	private int writePMTPacket() {
		long PMT_ID = TS_PMT_PID;
		long PCR_PID = TS_PCR_PID;
		int PMT_TABLE_OFFSET = 5;

		byte[] buffer = new byte[TS_PACKET_SIZE];
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) 0xff;
		}

		byte[] p = buffer;
		int tableLength = 18; // PMT 表数据内容长度, 不包括 PMT 表前 3 个字节
		int i = 0;

		// TS Packet Header (4 Bytes)
		p[i++] = TS_START_CODE; // 8 bit: 同步字符, 总是为 0x47
		p[i++] = (byte) (0x40 | ((PMT_ID >> 8) & 0x1F)); // 1 bit: 传输误码指示符, 1
															// bit: 起始指示符,
		// 1 bit: 优先传输
		p[i++] = (byte) (PMT_ID & 0xff); // 13 bit: PID
		p[i++] = (byte) (0x10 | fPMTContinuityCounter); // 2 bit: 传输加扰, 2 bit:
														// 自适应控制, 4
		// bit: 连续计数器
		fPMTContinuityCounter = (fPMTContinuityCounter + 1) & 0x0F;

		//
		p[i++] = 0x00; // 总是为 0x00

		// PMT Table
		p[i++] = TS_PMT_TABLE_ID; // 8 bit: 固定为0x02, 标志是该表是PMT
		p[i++] = (byte) (0xB0 | ((tableLength >> 8) & 0x0F)); // 1 bit:
																// 段语法标志位，固定为1;
																// 1
		// bit: 0; 2 bit: 保留
		// (1011 0000)
		p[i++] = (byte) (tableLength & 0xff); // 12 bit: 表示这个字节后面有用的字节数，包括CRC32
		p[i++] = 0x00; //
		p[i++] = 0x01; // 16 bit: 指出该节目对应于可应用的 Program map PID
		p[i++] = (byte) (0xC1); // 2 bit: 保留; 5 bit: 指出TS流中Program map
								// section的版本号
		// 1 bit: 当该位置1时，当前传送的 Program map section 可用 (1100
		// 0001)
		p[i++] = 0x00; // 8 bit: 固定为0x00
		p[i++] = 0x00; // 8 bit: 固定为0x00

		p[i++] = (byte) (0xE0 | ((PCR_PID >> 8) & 0x1F)); // 3 bit: 保留
		p[i++] = (byte) (PCR_PID & 0xff); // 13 bit: 节目号 指明 TS 包的PID值
		p[i++] = (byte) (0xF0); // 4 bit: 保留位
		p[i++] = 0x00; // 12 bit: 前两位bit为00。该域指出跟随其后对节目信息的描述的 byte 数

		// 视频流的描述
		p[i++] = STREAM_TYPE_VIDEO_H264; // H.264 视频流
		p[i++] = (byte) (0xE0 | ((TS_VIDEO_PID >> 8) & 0x1F)); //
		p[i++] = TS_VIDEO_PID & 0xff; //
		p[i++] = (byte) (0xF0); //
		p[i++] = 0x00; //

		// 32 位 CRC 校验码
		long crc = mpegts_crc32(buffer, PMT_TABLE_OFFSET, (tableLength + 3) - 4);
		p[i++] = (byte) ((crc >> 24) & 0xff);
		p[i++] = (byte) ((crc >> 16) & 0xff);
		p[i++] = (byte) ((crc >> 8) & 0xff);
		p[i++] = (byte) ((crc) & 0xff);

		writeTSPacket(buffer, TS_PACKET_SIZE);
		return 0;
	}

	/** 写入指定的 TS 包. */
	private int writeTSPacket(byte[] data, int length) {
		if (fIsStop) {
			// log.debug("writeTSPacket: fIsStop");
			return 0;
		}

		if (fOutputStream == null) {
			fOutputStream = getOutputStream();
			if (fOutputStream == null) {
				return 0;
			}

			fPacketIndex = 0;
		}

		if ((fPacketIndex % 30) == 0) {
			fPacketIndex++;

			writePATPacket();
			writePMTPacket();
		}

		try {
			fOutputStream.write(data, 0, length);

		} catch (IOException e) {
			log.debug(e.getMessage(), e);
			fIsStop = true;
			return 0;
		}

		return length;
	}

	public long writeVideoData(MMediaBuffer buffer) {
		if (buffer == null) {
			log.debug("empty sample");
			return 0;
		}

		ByteBuffer data = buffer.getData();
		int length = buffer.getSize();
		if (data == null || length <= 0) {
			log.debug("empty sample data");
			return 0;
		}

		boolean isEnd = buffer.isEnd();

		long pts = buffer.getSampleTime() / 1000 * 90 + TS_PTS_BASE;
		writeESPacket(data.duplicate(), length, pts, isEnd);

		return 0;
	}

}
