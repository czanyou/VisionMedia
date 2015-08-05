package org.vision.media.mp4;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class Mp4PropertyTest {
	@Test public void testProperty() throws IOException {

		Mp4Property property = new Mp4Property(Mp4PropertyType.PT_FLOAT, 4, "test");
		Mp4MemStream file = new Mp4MemStream(1024 * 640);
		file.seek(0);
		
		property.write(file);
		Assert.assertEquals(4, file.size());

		property = new Mp4Property(Mp4PropertyType.PT_BYTES, 10, "test");
		file.seek(0);
		property.write(file);
		Assert.assertEquals(10, file.size());

	}
}
