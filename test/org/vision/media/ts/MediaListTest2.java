package org.vision.media.ts;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaMuxer;
import org.vision.media.MMediaTypes;
import org.vision.media.hls.HttpLiveClient;
import org.vision.media.hls.HttpLiveClient.PlayStreamHandler;
import org.vision.media.mp4.Mp4Writer;

public class MediaListTest2 {

	protected static final Logger log = LoggerFactory
			.getLogger(MediaListTest2.class);

	private HttpLiveClient mHttpLiveClient;

	private int mFrameCount;
	private MMediaMuxer fWriter;

	@Test
	public void testData() throws IOException {
		byte value1 = (byte) 0xf0;
		int value = value1 & 0xff;

		mHttpLiveClient = new HttpLiveClient();

		Assert.assertEquals(0xf0, value);

		String httpUrl = "http://hzhls01.ys7.com:7885/hcnp/500383271_1_2_1_0_183.136.184.7_6500.m3u8?d35f8c69-e220-423e-a33c-84ea829097c3";

		mHttpLiveClient.setURL(httpUrl);

		mFrameCount = 0;

		int videoCodec = MMediaTypes.H264;
		int videoWidth = 512;
		int videoHeight = 288;

		// Dest
		MMediaMuxer writer = new Mp4Writer("data/record.mp4");
		MMediaFormat videoFormat = MMediaFormat.newVideoFormat(videoCodec,
				videoWidth, videoHeight);
		writer.setVideoFormat(videoFormat);
		writer.start();

		fWriter = writer;
		PlayStreamHandler handler = new PlayStreamHandler() {

			@Override
			public void onMediaSample(int flags, MMediaBuffer mediaBuffer) {
				boolean isSyncPoint = mediaBuffer.isSyncPoint();
				// boolean isEnd = mediaBuffer.isEnd();

				mFrameCount++;

				if (mFrameCount < 200) {
					log.debug(mFrameCount + ": time: "
							+ mediaBuffer.getSampleTime() + "/size."
							+ mediaBuffer.getSize());

					if (fWriter != null) {
						fWriter.writeVideoData(mediaBuffer);
					}

				} else {
					if (fWriter != null) {
						fWriter.stop();
						fWriter = null;
					}
				}
			}
		};

		mHttpLiveClient.setStreamHandler(handler);
		mHttpLiveClient.runLoop();

	}

}
