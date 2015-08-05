package org.vision.media.ts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.hls.PlayListReader;
import org.vision.media.hls.PlayListReader.PlayItem;

public class MediaListTest {

	protected static final Logger log = LoggerFactory
			.getLogger(MediaListTest.class);

	@Test
	public void testData() throws FileNotFoundException {
		byte value1 = (byte) 0xf0;
		int value = value1 & 0xff;

		Assert.assertEquals(0xf0, value);

		File file = new File("data/test.m3u8");

		PlayListReader reader = new PlayListReader();

		FileReader fileReader = new FileReader(file);
		reader.parsePlayList(fileReader);

		Assert.assertEquals(84, reader.getMediaSequence());
		Assert.assertEquals(3, reader.getTargetDuraction());

		List<PlayItem> playList = reader.getPlayList();
		Assert.assertEquals(3, playList.size());

		PlayItem playItem = playList.get(0);
		Assert.assertEquals(4, playItem.duration);
		Assert.assertEquals("http://192.168.1.21/html5/test.ts", playItem.url);
	}

}
