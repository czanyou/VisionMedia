package org.vision.media.hls;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaExtractor;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaTypes;
import org.vision.media.avc.Mp4VideoUtils;
import org.vision.media.mp4.Mp4MediaFactory.MediaFrameInfo;
import org.vision.media.mp4.Mp4Utils;

/**
 * 
 * @author m
 *
 */
public class MpegTSReader extends MMediaExtractor implements MpegTS {

	protected static final Logger log = LoggerFactory
			.getLogger(MpegTSReader.class);

	private MMediaBuffer fCurrentSample;

	private String fFileName;

	private InputStream fInputStream;

	private boolean fIsStart;

	private MMediaBuffer fNextSample;

	private byte[] fPacketBuffer = new byte[TS_PACKET_SIZE];

	@SuppressWarnings("unused")
	private int fPacketIndex;

	private int fPacketSize;

	private ByteBuffer fReadBuffer;

	@SuppressWarnings("unused")
	private int mAudioId = 0xff;

	private int mPmtId = 0xff;

	private MMediaFormat mVideoFormat;

	private int mVideoId = 0xff;

	public MpegTSReader(String filename) {
		fFileName = filename;
	}

	public boolean advance() {
		fCurrentSample = null;
		try {
			read();
			fNextSample = fCurrentSample;

		} catch (IOException e) {
			return false;
		}

		return fCurrentSample != null;
	}

	private int checkParamSets(ByteBuffer sampleData) {
		boolean isSyncPoint = false;

		while (true) {
			int position = sampleData.position();
			int headerSize = getStartCodeLength(sampleData);

			byte[] startCode = new byte[] { 0x00, 0x00, 0x00, 0x01 };
			if (headerSize == 3) {
				startCode = new byte[] { 0x00, 0x00, 0x01 };
			}

			int type = sampleData.get(position + headerSize) & 0x1f;
			if (type == 7 || type == 8 || type == 5) {
				isSyncPoint = true;

			} else {
				break;
			}

			int end = Mp4Utils.byteIndexOf(sampleData, startCode, 1);

			// log.debug("headerSize: " + headerSize + ", type=" + type +
			// ",end="
			// + end);
			if (type != 0 && type != 6 && type != 7 && type != 8) {
				break;
			}

			int size = sampleData.limit() - sampleData.position() - 4;
			if (end > 0) {
				size = end - sampleData.position() - 4;
			}

			if (type == 7) {
				byte[] bytes = new byte[size];
				sampleData.getInt();
				sampleData.get(bytes);
				saveVideoParamSets(bytes);
			}

			if (end > 0) {
				sampleData.position(end);

			} else {
				break;
			}
		}

		return isSyncPoint ? 1 : 0;
	}

	public void close() {
		fPacketIndex = 0;
		fReadBuffer = null;

		if (fInputStream != null) {
			try {
				fInputStream.close();
				fInputStream = null;

			} catch (IOException e) {
				log.debug(e.getMessage(), e);
			}
		}
	}

	public MMediaBuffer currentSample() {
		return fNextSample;
	}

	@Override
	public long getCachedDuration() {
		return 0;
	}

	@Override
	public long getDuration() {
		return 0;
	}

	private InputStream getInputStream() {
		if (fInputStream != null) {
			return fInputStream;
		}

		try {
			fInputStream = new FileInputStream(fFileName);
		} catch (FileNotFoundException e) {
			log.debug(e.getMessage(), e);
			return null;
		}

		return fInputStream;
	}

	private int getNextPacket() {
		if (fPacketSize > 0) {
			return fPacketSize;
		}

		InputStream inputStream = getInputStream();
		if (inputStream == null) {
			return -1;
		}

		try {
			int ret = inputStream.read(fPacketBuffer);
			if (ret < 0) {
				onClose();
				return -1;

			} else if (ret != TS_PACKET_SIZE) {
				log.error("getNextPacket: invalid packet size: " + ret);

				onClose();
				return -1;
			}

			if (fPacketBuffer[0] != TS_START_CODE) {
				log.error("getNextPacket: invalid packet header: "
						+ fPacketBuffer[0]);
				onClose();
				return -1;
			}

			fPacketSize = ret;
			return ret;

		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	public long getPosition() {
		return 0;
	}

	@Override
	public int getSampleFlags() {
		return 0;
	}

	@Override
	public long getSampleTime() {
		return 0;
	}

	@Override
	public int getSampleTrackIndex() {
		return 0;
	}

	private int getStartCodeLength(ByteBuffer sampleData) {
		int headerSize = 0;

		int pos = sampleData.position();
		if (sampleData.get(pos) != 0x00) {
			return 0;

		} else if (sampleData.get(pos + 1) != 0x00) {
			return 0;

		} else {
			headerSize = (sampleData.get(pos + 2) == 0x01) ? 3 : 4;
		}

		return headerSize;
	}

	@Override
	public int getTrackCount() {
		return 0;
	}

	public MMediaFormat getTrackFormat(int index) {
		return mVideoFormat;

	}

	@Override
	public boolean hasCacheReachedEndOfStream() {
		return false;
	}

	private void onClose() {

	}

	private void processPATPacket() {
		ByteBuffer buffer = ByteBuffer.wrap(fPacketBuffer);
		buffer.order(ByteOrder.BIG_ENDIAN);

		// TS header
		buffer.get();
		buffer.get();
		buffer.get();
		int value = buffer.get() & 0xff;
		int adaptation = (value >> 4) & 0x03;
		if (adaptation == 3) {
			int length = buffer.get() & 0xff;
			if (length >= 0 && length < 184) {
				int pos = buffer.position();
				buffer.position(pos + length);
			}
		}

		// PAT ID
		buffer.get();
		buffer.get(); // 0x00

		// PAT table
		value = buffer.getShort() & 0xffff;
		int length = value & 0x0fff;
		if (length < 9) {
			return;
		}

		// log.debug("pat length:" + length);

		//
		buffer.get(); // 16
		buffer.get(); // 16: 该传输流的ID
		buffer.get(); // 0xC1
		buffer.get(); // 0x00
		buffer.get(); // 0x00

		//
		buffer.get(); // 16
		buffer.get(); // 16: 节目号

		//
		value = buffer.getShort() & 0xffff;
		int pmtId = value & 0x1fff;
		// log.debug("read PMT ID:" + pmtId);

		mPmtId = pmtId;
	}

	@SuppressWarnings("unused")
	private void processPESPacket() {

		try {
			fReadBuffer.flip();
			fReadBuffer.mark();
			// log.debug("read buffer:" + fReadBuffer);

			// Start Code
			fReadBuffer.get();
			fReadBuffer.get();
			fReadBuffer.get();

			// Stream ID
			int streamID = fReadBuffer.get() & 0xff;
			if (streamID == 0xE0) {
				// log.debug("read stream id:" + streamID);
			}

			//
			fReadBuffer.get();
			fReadBuffer.get(); // 16: PES Length

			// Flags
			fReadBuffer.get();

			// PTS DTS
			fReadBuffer.get();
			long pts = 0;

			// Data Length
			int length = fReadBuffer.get() & 0xff;
			if (length > 0) {
				pts = readPTS(fReadBuffer);
				for (int i = 0; i < length; i++) {
					fReadBuffer.get();
				}

			} else {
				pts = 0;
				pts += (fReadBuffer.get() & 0x0e) << 30;
				pts += (fReadBuffer.get() & 0xff) << 22;
				pts += (fReadBuffer.get() & 0xfe) << 14;
				pts += (fReadBuffer.get() & 0xff) << 7;
				pts += (fReadBuffer.get() & 0xfe) >> 1;
				pts = (pts - TS_PTS_BASE) * 1000 / 90;
				log.debug("read pts:" + pts);

				long dts = 0;
				dts += (fReadBuffer.get() & 0x0e) << 29;
				dts += (fReadBuffer.get() & 0xff) << 22;
				dts += (fReadBuffer.get() & 0xfe) << 14;
				dts += (fReadBuffer.get() & 0xff) << 7;
				dts += (fReadBuffer.get() & 0xfe) >> 1;
				// log.debug("read dts:" + dts);
			}

			// au
			fReadBuffer.get();
			fReadBuffer.get();
			fReadBuffer.get();

			fReadBuffer.get();
			fReadBuffer.get();
			fReadBuffer.get();

			fReadBuffer.mark();

			int type = checkParamSets(fReadBuffer);
			boolean isSyncPoint = type > 0;

			fReadBuffer.reset();

			MMediaBuffer buffer = new MMediaBuffer();
			buffer.setData(fReadBuffer.duplicate());
			buffer.setEnd(true);
			buffer.setSampleTime(pts);
			buffer.setSyncPoint(isSyncPoint);

			fCurrentSample = buffer;

			fReadBuffer.clear();

		} catch (Exception e) {
			log.debug(e.getMessage());
		}
	}

	private void processPMTPacket() {
		ByteBuffer buffer = ByteBuffer.wrap(fPacketBuffer);
		buffer.order(ByteOrder.BIG_ENDIAN);

		// TS header
		buffer.get();
		buffer.get();
		buffer.get();
		int value = buffer.get() & 0xff;
		int adaptation = (value >> 4) & 0x03;
		if (adaptation == 3) {

			int length = buffer.get() & 0xff;
			if (length > 0 && length < 184) {
				int pos = buffer.position();
				buffer.position(pos + length);
			}
		}

		// PMT ID
		value = buffer.get() & 0xff;
		value = buffer.get() & 0xff;

		// PMT table
		value = buffer.getShort() & 0xffff;
		int length = value & 0x0fff;
		if (length < 17) {
			return;
		}

		buffer.get();
		buffer.get();
		buffer.get();

		buffer.get();
		buffer.get();

		buffer.get();
		buffer.get();
		buffer.get();
		buffer.get();

		int codecId = buffer.get() & 0xff;
		value = buffer.getShort() & 0xffff;
		int streamId = value & 0x1fff;

		value = buffer.get() & 0xff;
		value = buffer.get() & 0xff;

		if (codecId == STREAM_TYPE_VIDEO_H264) {
			mVideoId = streamId;
			// log.debug("read videoId:" + streamId + "," + length);

		} else if (codecId == STREAM_TYPE_AUDIO_AAC) {
			mAudioId = streamId;
			// log.debug("read audioId:" + streamId + "," + length);
		}

		if (length >= 21) {
			codecId = buffer.get() & 0xff;
			value = buffer.getShort() & 0xffff;
			streamId = value & 0x1fff;

			value = buffer.get() & 0xff;
			value = buffer.get() & 0xff;

			if (codecId == STREAM_TYPE_VIDEO_H264) {
				mVideoId = streamId;
				// log.debug("read videoId:" + streamId + "," + length);

			} else if (codecId == STREAM_TYPE_AUDIO_AAC) {
				mAudioId = streamId;
				// log.debug("read audioId:" + streamId + "," + length);
			}

		}
	}

	@SuppressWarnings("unused")
	private void processVideoPacket() {
		if (fIsStart) {
			// log.debug("isStart:" + fIsStart + "(" + fPacketIndex + ")");

			if (fReadBuffer != null) {
				fReadBuffer.clear();
			}
		}

		boolean isAdaptationField = (fPacketBuffer[3] & 0x20) != 0;
		int size = TS_PAYLOAD_SIZE;
		int offset = 4;
		if (isAdaptationField) {
			// log.debug("isAdaptationField:" + isAdaptationField + "(" +
			// fPacketIndex + ")");
			int stuffing = (fPacketBuffer[4] & 0xff) + 1;
			// log.debug("read stuffing:" + stuffing);

			if (stuffing > 8) {
				// int flags = fPacketBuffer[5] & 0xff;
				// log.debug("read flags:" + flags);

				long pcr = readPCR(fPacketBuffer, 6);
				// log.debug("read pcr:" + pcr);

				size -= stuffing;
				offset += stuffing;
			}
		}

		if (fReadBuffer == null) {
			fReadBuffer = ByteBuffer.allocate(1024 * 256);

		} else {
			int freeSize = fReadBuffer.limit() - fReadBuffer.position();
			if (freeSize <= size) {
				int newSize = fReadBuffer.capacity() * 2;
				ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
				fReadBuffer.flip();

				newBuffer.put(fReadBuffer);
				fReadBuffer = newBuffer;

				// log.warn("newSize:" + newSize);
			}
		}

		fReadBuffer.put(fPacketBuffer, offset, size);
	}

	private void read() throws IOException {
		while (fCurrentSample == null) {
			int ret = getNextPacket();
			if (ret < 0) {
				if (fReadBuffer != null && fReadBuffer.position() > 0) {
					processPESPacket();
				}
				break;
			}

			int value = fPacketBuffer[1] & 0xff;
			value = value << 8;
			value += fPacketBuffer[2] & 0xff;

			int pid = value & 0x1fff;
			fIsStart = (value & 0x4000) != 0;
			if (fIsStart) {
				fPacketIndex = 0;

				if (fReadBuffer != null && fReadBuffer.position() > 0) {
					processPESPacket();

					break;
				}
			}

			if (pid == mVideoId) {
				processVideoPacket();

			} else if (pid == TS_PAT_PID) {
				processPATPacket();

			} else if (pid == mPmtId) {
				processPMTPacket();
			}

			skipNextPacket();
		}
	}

	private long readPCR(byte[] buffer, int offset) {

		long pcr = 0;
		pcr += (buffer[offset + 0] & 0xff) << 25;
		pcr += (buffer[offset + 1] & 0xff) << 17;
		pcr += (buffer[offset + 2] & 0xff) << 9;
		pcr += (buffer[offset + 3] & 0xff) << 1;

		long pts = (pcr - TS_PTS_BASE) * 1000 / 90;
		return pts;
	}

	private long readPTS(ByteBuffer buffer) {
		buffer.mark();

		long pts = 0;
		// hight int
		pts = (buffer.get() & 0x0e);
		pts = pts << 30;
		
		// low int
		pts += (buffer.get() & 0xff) << 22;
		pts += (buffer.get() & 0xfe) << 14;
		pts += (buffer.get() & 0xff) << 7;
		pts += (buffer.get() & 0xfe) >> 1;
	
		pts = (pts - TS_PTS_BASE) * 1000 / 90;
		buffer.reset();
		return pts;
	}

	@Override
	public int readSampleData(ByteBuffer byteBuffer, int offset) {
		return 0;
	}

	private long saveVideoParamSets(byte[] sampleData) {
		if (sampleData == null) {
			return 0;

		} else if (sampleData.length < 4) {
			return 0;
		}

		int type = Mp4VideoUtils.getNaluType(sampleData, 0);

		// log.debug("sampleSize: " + sampleData.length + ", type=" + type);

		if (type == Mp4VideoUtils.H264_NAL_TYPE_SEQ_PARAM) {
			MediaFrameInfo frameInfo = Mp4VideoUtils.parseSeqInfo(sampleData);

			// log.debug("videoWidth: " + frameInfo.videoWidth +
			// ", videoHeight="
			// + frameInfo.videoHeight);

			if (mVideoFormat == null) {
				mVideoFormat = MMediaFormat.newVideoFormat(MMediaTypes.H264,
						frameInfo.videoWidth, frameInfo.videoHeight);
			} else {
				mVideoFormat.setVideoWidth(frameInfo.videoWidth);
				mVideoFormat.setVideoHeight(frameInfo.videoHeight);
			}
		}

		return 0;
	}

	@Override
	public void seekTo(long time, int flags) {

	}

	@Override
	public void selectTrack(int index) {

	}

	@Override
	public void setDataSource(String path) throws IOException {

	}

	public void setFilename(String filename) {
		fFileName = filename;
	}

	public void setInputStream(ByteBufferInputStream inputStream) {
		fInputStream = inputStream;
	}

	private void skipNextPacket() {
		if (fPacketSize > 0) {
			fPacketSize = 0;

			fPacketIndex++;
		}
	}

	@Override
	public void unselectTrack(int index) {

	}
}
