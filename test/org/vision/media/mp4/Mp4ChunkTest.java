package org.vision.media.mp4;

import org.junit.Assert;
import org.junit.Test;

public class Mp4ChunkTest {

	@Test
	public void testChunk() {
		Mp4Chunk chunk = new Mp4Chunk();
		Mp4Sample sample = new Mp4Sample();
		sample.setSampleSize(512);
		sample.setSampleDuration(40);
		
		chunk.addSample(sample);
		
		Assert.assertEquals(512, chunk.getSize());
		Assert.assertEquals(40, chunk.getDuration());
		Assert.assertEquals(1, chunk.getSampleCount());
		Assert.assertEquals(false, chunk.isEmpty());
	}
}
