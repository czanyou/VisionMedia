package org.vision.media.mp4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaExtractor;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaMuxer;
import org.vision.media.MMediaTypes;

public class Mp4LiveTest {
	
	private static final Logger log = LoggerFactory
			.getLogger(Mp4LiveTest.class);

	@SuppressWarnings("unused")
	@Test
	public void testWriter2() throws IOException {
		File file = new File("live.mp4");
		file.delete();

		int videoTrackIndex = 1;
		int audioTrackIndex = 0;

		// Source
		MMediaExtractor reader = new Mp4Reader();
		reader.setDataSource("test.mp4");

		MMediaFormat readerFormat = reader.getTrackFormat(0);
		if (MMediaTypes.VIDEO.equals(readerFormat.getMediaType())) {
			videoTrackIndex = 0;
			audioTrackIndex = 1;
		}

		MMediaFormat readerVideoFormat = reader.getTrackFormat(videoTrackIndex);
		int videoWidth = readerVideoFormat.getVideoWidth();
		int videoHeight = readerVideoFormat.getVideoHeight();

		MMediaFormat readerAudioFormat = reader.getTrackFormat(audioTrackIndex);
		int audioTimeScale = readerAudioFormat.getSampleRate();

		int videoCodec = MMediaTypes.H264;
		int audioCodec = MMediaTypes.AAC;

		log.debug("videoFormat: scale=" + videoWidth + "x" + videoHeight + "/"
				+ videoCodec);

		ByteBuffer readBuffer = ByteBuffer.allocate(videoWidth * videoHeight
				* 2);

		// Dest
		MMediaMuxer writer = new Mp4StreamWriter("live.mp4");
		MMediaFormat videoFormat = MMediaFormat.newVideoFormat(videoCodec,
				videoWidth, videoHeight);
		writer.setVideoFormat(videoFormat);

		writer.start();

		// Samples
		int count = 0;
		while (true) {
			if (!reader.advance()) {
				break;
			}

			int trackIndex = reader.getSampleTrackIndex();
			long sampleTime = reader.getSampleTime();
			int sampleFlags = reader.getSampleFlags();
			boolean isSyncSample = (sampleFlags & MMediaExtractor.FLAG_SYNC_SAMPLE) != 0;

			readBuffer.clear();
			int sampleSize = reader.readSampleData(readBuffer, 0);
			if (sampleSize <= 0) {
				break;
			}

			if (count < 10) {
				log.debug(trackIndex + ": sampleSize: " + sampleSize + "/"
						+ readBuffer + ",sync=" + (sampleTime / 1000));
			}

			if (trackIndex == videoTrackIndex) {
				MMediaBuffer mediaBuffer = new MMediaBuffer();
				mediaBuffer.setData(readBuffer);
				mediaBuffer.setSyncPoint(isSyncSample);
				mediaBuffer.setEnd(true);
				mediaBuffer.setSampleTime(sampleTime);
				writer.writeVideoData(mediaBuffer);
			}

			count++;
		}

		log.debug("End streaming " + count);

		// close
		writer.stop();
		writer = null;

		reader.close();
	}
	
	@Test
	public void testStream2() throws IOException {
		Mp4Reader reader = new Mp4Reader();
		reader.setDataSource("live.mp4");

		// track Count
		int trackCount = reader.getTrackCount();
		assertEquals(1, trackCount);

		// video Format
		MMediaFormat videoFormat = reader.getTrackFormat(0);
		assertNotNull(videoFormat);
		log.debug("format:" + videoFormat);

		Mp4Track videoTrack = reader.getTrack("vide");
		assertNotNull(videoTrack);
		log.debug("videoTrack:" + videoTrack);

		Mp4Atom track = videoTrack.getTrackAtom();
		log.debug("track:" + track);

		Mp4Atom atom = reader.getRootAtom();
		Mp4ReaderTest.printAtom(atom);

	}
}
