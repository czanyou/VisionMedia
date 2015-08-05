package org.vision.media.ts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaMuxer;
import org.vision.media.MMediaTypes;
import org.vision.media.hls.MpegTSReader;
import org.vision.media.mp4.Mp4Reader;
import org.vision.media.mp4.Mp4Track;
import org.vision.media.mp4.Mp4Writer;

public class MediaReadTest {

	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory
			.getLogger(MediaReadTest.class);

	public void testFile() throws IOException {
		Mp4Reader reader = new Mp4Reader();
		reader.setDataSource("data/hd.out.mp4");

		// track Count
		int trackCount = reader.getTrackCount();
		assertEquals(1, trackCount);

		int videoTrackIndex = 0;

		// video Format
		MMediaFormat videoFormat = reader.getTrackFormat(videoTrackIndex);
		assertNotNull(videoFormat);

		assertEquals(MMediaTypes.VIDEO, videoFormat.getMediaType());
		assertEquals(MMediaTypes.H264, videoFormat.getCodecType());
		assertEquals(1920, videoFormat.getVideoWidth());
		assertEquals(1080, videoFormat.getVideoHeight());
		assertEquals(15000, videoFormat.getSampleRate());
		assertEquals(0, videoFormat.getChannelCount());

		reader.selectTrack(videoTrackIndex);

		// Video Tracks
		Mp4Track videoTrack = reader.getTrack("vide");
		assertNotNull(videoTrack);

		assertEquals(9, videoTrack.getChunkCount());
		// assertEquals(3511800, videoTrack.getAvgBitrate());
		// assertEquals(125400, videoTrack.getDuration());
		// assertEquals(4214160, videoTrack.getMaxBitrate());
		// assertEquals(281957, videoTrack.getMaxSampleSize());
		assertEquals(209, videoTrack.getSampleCount());
		assertEquals(15000, videoTrack.getTimeScale());
		assertEquals(1, videoTrack.getTrackId());
		assertEquals("vide", videoTrack.getType());
		assertEquals(1920, videoTrack.getVideoWidth());
		assertEquals(1080, videoTrack.getVideoHeight());
	}

	@Test
	public void testReader() throws IOException {

		MpegTSReader reader = new MpegTSReader("data/stream.ts");
		// MpegTSReader reader = new MpegTSReader("data/hd.ts");

		int videoCodec = MMediaTypes.H264;
		int videoWidth = 1920;
		int videoHeight = 1080;

		// Dest
		MMediaMuxer writer = new Mp4Writer("data/hd.out.mp4");
		MMediaFormat videoFormat = MMediaFormat.newVideoFormat(videoCodec,
				videoWidth, videoHeight);
		writer.setVideoFormat(videoFormat);

		writer.start();

		int count = 0;
		while (true) {
			if (!reader.advance()) {
				break;
			}

			MMediaBuffer mediaBuffer = reader.currentSample();

			boolean isSyncPoint = mediaBuffer.isSyncPoint();
			boolean isEnd = mediaBuffer.isEnd();
			count++;

			if (isSyncPoint) {
				log.debug(count + ": time: " + mediaBuffer.getSampleTime()
						+ "/size." + mediaBuffer.getSize());
			}

			writer.writeVideoData(mediaBuffer);

		}

		log.debug("count: " + count);

		writer.stop();
		writer = null;

		testFile();
	}

	@Test
	public void testData() {
		byte value1 = (byte) 0xf0;
		int value = value1 & 0xff;

		Assert.assertEquals(0xf0, value);

	}
}
