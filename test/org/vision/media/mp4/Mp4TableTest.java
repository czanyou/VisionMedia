package org.vision.media.mp4;

import org.junit.Test;
import org.vision.media.mp4.Mp4TableProperty;

public class Mp4TableTest {
	@Test public void testTable() {
		Mp4TableProperty table = new Mp4TableProperty("");
		table.addColumn("a");
	}
}
