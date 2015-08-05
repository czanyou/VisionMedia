package org.vision.media.mp4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.vision.media.mp4.Mp4PropertyType.PT_FLOAT;
import static org.vision.media.mp4.Mp4PropertyType.PT_INT;
import static org.vision.media.mp4.Mp4PropertyType.PT_STRING;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaTypes;

public class Mp4WriterTest {

	@Test
	public void testFilePath() throws IOException {
		File file = new File("test.bin");
		System.out.print(file.getAbsoluteFile());
	}

	@Test
	public void testWriter() throws IOException {
		// source data
		File file = new File("test.bin");
		assertTrue(file.exists());

		// dest file
		file = new File("avc.mp4");
		file.delete();

		// start
		Mp4Writer writer = new Mp4Writer("avc.mp4");
		MMediaFormat mediaFormat = MMediaFormat.newVideoFormat(
				MMediaTypes.H264, 720, 288);
		writer.setVideoFormat(mediaFormat);
		writer.start();

		Mp4Stream rawStream = new Mp4FileStream("test.bin", true);
		while (true) {
			// size header
			int code = rawStream.read();
			if (code != '$') {
				break;
			}
			rawStream.read();
			int size = rawStream.readInt16();
			if (size <= 0 || size > 1024 * 1024) {
				break;
			}

			// sample data
			byte data[] = new byte[size];
			rawStream.readBytes(data);

			// RTP
			boolean isSync = false;
			if (data[12] == 0x67) {
				isSync = true;
			}

			// RTP payload
			ByteBuffer buffer = ByteBuffer.wrap(data, 12, data.length - 12);

			// sample
			MMediaBuffer sample = new MMediaBuffer();
			sample.setData(buffer);
			sample.setSyncPoint(isSync);
			sample.setEnd(true);
			sample.setSampleTime(10000 / 10);
			writer.writeVideoData(sample);
		}

		writer.stop();
	}

	@Test
	public void testWriteAtom() throws IOException {
		// init Atom
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("udat");
		atom.addProperty(PT_INT, 4, "int");
		atom.addProperty(PT_FLOAT, 2, "float");
		atom.addProperty(PT_STRING, 8, "string");
		atom.setProperty("int", 199);
		atom.setProperty("float", 29.2f);
		atom.setProperty("string", "abcd1234");

		// writeAtom
		Mp4MemStream mp4File = new Mp4MemStream(1024 * 640);
		Mp4Moive.writeAtom(mp4File, atom);
		assertEquals(22, mp4File.size());

		// assertEquals
		mp4File.buffer.position(0);
		assertEquals(22, mp4File.readInt32());
		assertEquals(Mp4Atom.getAtomId("udat"), mp4File.readInt32());
		assertEquals(199, mp4File.readInt32());
		mp4File.skip(2);
		assertEquals('a', mp4File.read());
	}

	@Test
	public void testWriteProperties() throws IOException {
		// init Atom
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("udat");
		atom.addProperty(PT_INT, 4, "int");
		atom.addProperty(PT_FLOAT, 2, "float");
		atom.addProperty(PT_STRING, 8, "string");
		atom.setProperty("int", 199);
		atom.setProperty("float", 29.2f);
		atom.setProperty("string", "abcd1234");

		// writeAtom
		Mp4MemStream mp4File = new Mp4MemStream(1024 * 640);
		Mp4Moive.writeAtom(mp4File, atom);

		// assertEquals
		mp4File.buffer.position(0);

		// header
		assertEquals(22, mp4File.readInt32());
		assertEquals(Mp4Atom.getAtomId("udat"), mp4File.readInt32());

		// int
		assertEquals(0, mp4File.read());
		assertEquals(0, mp4File.read());
		assertEquals(0, mp4File.read());
		assertEquals(199, mp4File.read());

		// float
		assertEquals(29, mp4File.read());
		mp4File.read();

		// string
		assertEquals('a', mp4File.read());
		assertEquals('b', mp4File.read());
		assertEquals('c', mp4File.read());
		assertEquals('d', mp4File.read());
		assertEquals('1', mp4File.read());
		assertEquals('2', mp4File.read());
		assertEquals('3', mp4File.read());
		assertEquals('4', mp4File.read());
	}
}
