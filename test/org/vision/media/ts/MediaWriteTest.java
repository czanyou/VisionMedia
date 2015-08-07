package org.vision.media.ts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaExtractor;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaTypes;
import org.vision.media.hls.MpegTSWrtier;
import org.vision.media.mp4.Mp4Reader;
import org.vision.media.mp4.Mp4Track;

public class MediaWriteTest {
	private static final Logger log = LoggerFactory
			.getLogger(MediaWriteTest.class);

	@SuppressWarnings("unused")
	@Test
	public void testReader() throws IOException {
		Mp4Reader reader = new Mp4Reader();
		reader.setDataSource("data/hd1080.mp4");

		// track Count
		int trackCount = reader.getTrackCount();
		assertEquals(1, trackCount);

		int videoTrackIndex = 0;
		int audioTrackIndex = 1;

		// video Format
		MMediaFormat videoFormat = reader.getTrackFormat(videoTrackIndex);
		assertNotNull(videoFormat);
		assertEquals(MMediaTypes.VIDEO, videoFormat.getMediaType());
		assertEquals(MMediaTypes.H264, videoFormat.getCodecType());
		assertEquals(1920, videoFormat.getVideoWidth());
		assertEquals(1080, videoFormat.getVideoHeight());
		assertEquals(25, videoFormat.getSampleRate());
		assertEquals(0, videoFormat.getChannelCount());

		reader.selectTrack(videoTrackIndex);

		// Video Tracks
		Mp4Track videoTrack = reader.getTrack("vide");
		assertNotNull(videoTrack);
		assertEquals(30, videoTrack.getChunkCount());
		assertEquals(3511800, videoTrack.getAvgBitrate());
		assertEquals(209, videoTrack.getDuration());
		assertEquals(4214160, videoTrack.getMaxBitrate());
		assertEquals(281957, videoTrack.getMaxSampleSize());
		assertEquals(209, videoTrack.getSampleCount());
		assertEquals(25, videoTrack.getTimeScale());
		assertEquals(1, videoTrack.getTrackId());
		assertEquals("vide", videoTrack.getType());
		assertEquals(1920, videoTrack.getVideoWidth());
		assertEquals(1080, videoTrack.getVideoHeight());

		//
		MpegTSWrtier wrtier = new MpegTSWrtier("data/output/hd1080.ts");
		
		int width = videoFormat.getVideoWidth();
		int height = videoFormat.getVideoHeight();
		int bufferSize = width * height * 2;
		ByteBuffer readBuffer = ByteBuffer.allocate(bufferSize);

		int i = 0;
		while (reader.advance()) {
			long sampleTime = reader.getSampleTime();
			int sampleFlags = reader.getSampleFlags();
			boolean isSyncPoint = (sampleFlags & MMediaExtractor.FLAG_SYNC_SAMPLE) != 0;

			readBuffer.clear();
			int sampleSize = reader.readSampleData(readBuffer, 0);
			i++;
			if (!isSyncPoint) {
				log.debug(i + ": sampleSize: " + sampleSize + "/" + readBuffer
						+ ",sampleTime=" + sampleTime);
			}

			if (sampleSize <= 0) {
				break;
			}

			MMediaBuffer buffer = new MMediaBuffer();
			buffer.setEnd(true);
			buffer.setSampleTime(sampleTime);
			buffer.setSyncPoint(isSyncPoint);
			buffer.setData(readBuffer);
			wrtier.writeVideoData(buffer);
		}

		log.debug("count: " + i);

		wrtier.stop();

		reader.close();
	}
}
