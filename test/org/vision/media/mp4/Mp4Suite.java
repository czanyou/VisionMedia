package org.vision.media.mp4;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ Mp4Test.class, Mp4AtomTest.class, Mp4TrackTest.class,
		Mp4ReaderTest.class, Mp4WriterTest.class, Mp4StreamTest.class,
		Mp4PropertyTest.class, Mp4TableTest.class, Mp4ChunkTest.class })
public class Mp4Suite {

}
