package org.vision.media.ts;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaMuxer;
import org.vision.media.MMediaTypes;
import org.vision.media.avc.Mp4VideoUtils;
import org.vision.media.avc.Mp4VideoUtils.SampleInfo;
import org.vision.media.hls.HttpLiveClient;
import org.vision.media.hls.HttpLiveClient.PlayStreamHandler;
import org.vision.media.mp4.Mp4MediaFactory.MediaFrameInfo;
import org.vision.media.mp4.Mp4Writer;

public class MediaListTest2 {

	protected static final Logger log = LoggerFactory
			.getLogger(MediaListTest2.class);

	private HttpLiveClient mHttpLiveClient;

	private int mFrameCount;
	private MMediaMuxer fWriter;

	private long mStartTime = 0;

	private boolean mIsStarted;

	public MMediaFormat getMMediaFormat(MMediaBuffer mediaBuffer) {
		int videoCodec = MMediaTypes.H264;
		int videoWidth = 512;
		int videoHeight = 288;

		ByteBuffer buffer = mediaBuffer.getData();

		SampleInfo info = Mp4VideoUtils.parseSampleInfo(buffer);
		log.debug("getMMediaFormat: type: " + info.naluType + "/"
				+ info.startCodeLength);
		log.debug("getMMediaFormat: pps: " + info.ppsData);
		log.debug("getMMediaFormat: seq: " + info.seqData);

		MediaFrameInfo frameInfo = Mp4VideoUtils.parseSeqInfo(info.seqData
				.array());
		log.debug("getMMediaFormat: w: " + frameInfo.videoWidth);
		log.debug("getMMediaFormat: h: " + frameInfo.videoHeight);
		
		videoWidth = frameInfo.videoWidth;
		videoHeight = frameInfo.videoHeight;

		return MMediaFormat.newVideoFormat(videoCodec, videoWidth, videoHeight);
	}

	@Test
	public void testData() throws IOException {
		byte value1 = (byte) 0xf0;
		int value = value1 & 0xff;

		mHttpLiveClient = new HttpLiveClient();

		Assert.assertEquals(0xf0, value);

		String httpUrl = "http://hzhls01.ys7.com:7885/hcnp/097216984_1_2_1_0_183.136.184.7_6500.m3u8?d5599c00-4397-48f9-8f24-b9e21593d045";

		httpUrl = "http://live1.chinavtech.cn:8097/live/WGKJ002044CKFBC/stream.m3u8";
		mHttpLiveClient.setURL(httpUrl);

		mFrameCount = 0;
		mStartTime = 0;
		mIsStarted = false;

		// Dest
		MMediaMuxer writer = new Mp4Writer("data/record.mp4");

		fWriter = writer;
		PlayStreamHandler handler = new PlayStreamHandler() {

			@Override
			public void onMediaSample(int flags, MMediaBuffer mediaBuffer) {
				boolean isSyncPoint = mediaBuffer.isSyncPoint();
				// boolean isEnd = mediaBuffer.isEnd();

				if (mStartTime == 0) {
					mStartTime = mediaBuffer.getSampleTime();
				}

				long time = (mediaBuffer.getSampleTime() - mStartTime) / 1000;
				log.debug(mFrameCount + ": time: " + time + "/" + mediaBuffer);

				mFrameCount++;
				if (isSyncPoint) {
					try {
						if (!mIsStarted) {
							fWriter.setVideoFormat(getMMediaFormat(mediaBuffer));
							fWriter.start();
							mIsStarted = true;
						}

					} catch (Exception e) {

					}
				}

				if (mFrameCount < 200) {
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
