package org.vision.media.mp4;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mp4StreamTest {

	private static final Logger log = LoggerFactory
			.getLogger(Mp4StreamTest.class);

	@Test
	public void testStream1() throws IOException {
		Mp4MemStream file = new Mp4MemStream(1024 * 640);
		file.write(0);
		Assert.assertEquals(1, file.size());

		file.seek(0);
		file.writeInt(1, 1);
		Assert.assertEquals(1, file.size());

		file.seek(0);
		file.writeInt(1, 2);
		Assert.assertEquals(2, file.size());

		file.seek(0);
		file.writeInt(1, 3);
		Assert.assertEquals(3, file.size());

		file.seek(0);
		file.writeInt(1, 4);
		Assert.assertEquals(4, file.size());

		file.seek(0);
		file.writeInt(1, 8);
		Assert.assertEquals(8, file.size());

		file.seek(0);
		file.write(new byte[188]);
		Assert.assertEquals(188, file.size());
	}


}
