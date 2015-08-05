package org.vision.media.mp4;

import java.io.File;
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

public class Mp4MpegTest {

	private static final Logger log = LoggerFactory
			.getLogger(Mp4MpegTest.class);

	@Test
	public void testWriter2() throws IOException {
		File file = new File("live.mp4");
		file.delete();

		int videoTrackIndex = 1;

		// Source
		MMediaExtractor reader = new Mp4Reader();
		reader.setDataSource("test.mp4");

		MMediaFormat readerFormat = reader.getTrackFormat(0);
		if (MMediaTypes.VIDEO.equals(readerFormat.getMediaType())) {
			videoTrackIndex = 0;
		}

		MMediaFormat readerVideoFormat = reader.getTrackFormat(videoTrackIndex);
		int videoWidth = readerVideoFormat.getVideoWidth();
		int videoHeight = readerVideoFormat.getVideoHeight();

		int videoCodec = MMediaTypes.H264;

		log.debug("videoFormat: scale=" + videoWidth + "x" + videoHeight + "/"
				+ videoCodec);

		ByteBuffer readBuffer = ByteBuffer.allocate(videoWidth * videoHeight
				* 2);

		// Dest
		MpegTSWrtier writer = new MpegTSWrtier("test1.ts");
		int index = 1;

		long lastTime = 0;

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

			if (count < 0) {
				log.debug(trackIndex + ": sampleSize: " + sampleSize + "/"
						+ readBuffer + ",sync=" + isSyncSample);
			}

			if (trackIndex == videoTrackIndex) {
				if (isSyncSample) {
					writer.stop();

					long span = (sampleTime - lastTime) / 1000;

					log.debug(trackIndex + ": sampleSize: " + sampleSize + "/"
							+ span + ",sync=" + isSyncSample);

					lastTime = sampleTime;
					String filename = "test" + (index++) + ".ts";
					writer.setFilename(filename);
				}

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

}
