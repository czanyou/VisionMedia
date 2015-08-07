package org.vision.media.mp4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.vision.media.mp4.Mp4PropertyType.PT_FLOAT;
import static org.vision.media.mp4.Mp4PropertyType.PT_INT;
import static org.vision.media.mp4.Mp4PropertyType.PT_STRING;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaExtractor;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaMuxer;
import org.vision.media.MMediaTypes;

public class Mp4ReaderTest {

	private static final Logger log = LoggerFactory
			.getLogger(Mp4ReaderTest.class);

	public static void printAtom(Mp4Atom atom) {
		int depth = atom.getDepth();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= depth; i++) {
			sb.append(" -");
		}

		System.out.println(sb + " " + atom.getTypeString() + "("
				+ atom.getSize() + ")");
		printAtomInfo(atom);

		for (Mp4Atom child : atom.getChildAtoms()) {
			printAtom(child);
		}
	}

	private static void printAtomInfo(Mp4Atom atom) {
		// System.out.println("ATOM:" + atom.getTypeString() + "(" +
		// atom.getSize() + ")");

		for (Mp4Property property : atom.getProperties()) {
			printProperty(property);
		}
	}

	private static void printProperty(Mp4Property property) {
		System.out.println("          # " + property.getName() + "("
				+ property.getSize() + ") = [" + property.getValueString()
				+ "]");
	}

	public void getValue(StringBuilder sb, byte[] data) {
		for (int i = 0; i < data.length; i++) {
			sb.append(String.format("%02X ", data[i]));
		}
	}

	public boolean startsWith(byte[] buffer, int position, byte[] code) {

		return true;
	}

	@SuppressWarnings("unused")
	public void testReader() throws IOException {
		Mp4Reader reader = new Mp4Reader();
		reader.setDataSource("output.mp4");

		// track Count
		int trackCount = reader.getTrackCount();
		assertEquals(2, trackCount);

		int videoTrackIndex = 0;
		int audioTrackIndex = 1;

		// video Format
		MMediaFormat videoFormat = reader.getTrackFormat(videoTrackIndex);
		assertNotNull(videoFormat);

		assertEquals("video", videoFormat.getMediaType());
		assertEquals(MMediaTypes.H264, videoFormat.getCodecType());
		assertEquals(320, videoFormat.getVideoWidth());
		assertEquals(240, videoFormat.getVideoHeight());
		assertEquals(15000, videoFormat.getSampleRate());

		// samples
		for (int i = 0; i < 10; i++) {
			Mp4Sample nextSample = reader.getSample();
			if (nextSample == null) {
				break;
			}
			assertNotNull(nextSample);

			log.debug(i + ": " + nextSample.getTrackIndex() + ":"
					+ nextSample.getSampleId() + "/"
					+ nextSample.getSampleTime() + "/"
					+ nextSample.getSampleSize());

			reader.advance();
		}

		reader.selectTrack(videoTrackIndex);

		// Tracks
		Mp4Track videoTrack = reader.getTrack("vide");
		assertNotNull(videoTrack);

		// ������ Sample
		Mp4Sample sample = reader.readSample(videoTrackIndex, 1);

		ByteBuffer data = sample.getData();
		byte[] bytes = data.array();

		assertNotNull(sample);
		assertNotNull(bytes);
		assertEquals(4100, bytes.length);
		assertEquals(0, sample.getSampleTime());
		assertEquals(true, sample.isSyncPoint());

		assertEquals(0x00, bytes[0]);
		assertEquals(0x00, bytes[1]);
		assertEquals(0x02, bytes[2]);
		assertEquals(0x08, bytes[3]);

		// �� 10 �� Sample
		sample = reader.readSample(videoTrackIndex, 10);
		data = sample.getData();
		bytes = data.array();

		assertNotNull(sample);
		assertNotNull(bytes);
		assertEquals(1866, bytes.length);
		assertEquals(600003, sample.getSampleTime());
		assertEquals(false, sample.isSyncPoint());

		Mp4Atom trak = videoTrack.getTrackAtom();
		assertNotNull(trak);

		// Track Id
		Mp4Property property = trak.findProperty("tkhd.trackId");
		assertEquals(1, property.getValueInt());

		// STSC
		property = trak.findProperty("mdia.minf.stbl.stsc.entries");
		assertNotNull(property);
		assertEquals(Mp4PropertyType.PT_TABLE, property.getType());

		Mp4TableProperty table = (Mp4TableProperty) property;
		// print(table);

		property = trak
				.findProperty("mdia.minf.stbl.stsd.avc1.avcC.sequenceEntries");
		assertNotNull(property);
		assertEquals(Mp4PropertyType.PT_SIZE_TABLE, property.getType());

		Mp4SizeTableProperty sizeTable = (Mp4SizeTableProperty) property;
		// print(sizeTable);

		property = trak
				.findProperty("mdia.minf.stbl.stsd.avc1.avcC.pictureEntries");
		assertNotNull(property);
		assertEquals(Mp4PropertyType.PT_SIZE_TABLE, property.getType());

		sizeTable = (Mp4SizeTableProperty) property;
		// print(sizeTable);

		reader.close();
	}

	@Test
	public void testReader3() throws IOException {
		Mp4Reader reader = new Mp4Reader();
		reader.setDataSource("car.mp4");

		// track Count
		int trackCount = reader.getTrackCount();
		assertEquals(1, trackCount);

		// video Format
		MMediaFormat videoFormat = reader.getTrackFormat(0);
		assertNotNull(videoFormat);
		log.debug("format:" + videoFormat);

		Mp4Track videoTrack = reader.getTrack("vide");
		assertNotNull(videoTrack);
		log.debug("videoTrack:" + videoTrack);

		Mp4Atom track = videoTrack.getTrackAtom();
		log.debug("track:" + track);
		
		Mp4Atom atom = reader.getRootAtom();
		printAtom(atom);

	}

	@SuppressWarnings("unused")
	public void testReader2() throws IOException {
		Mp4Reader reader = new Mp4Reader();
		reader.setDataSource("car.mp4");

		// track Count
		int trackCount = reader.getTrackCount();
		assertEquals(2, trackCount);

		// video Format
		MMediaFormat videoFormat = reader.getTrackFormat(0);
		assertNotNull(videoFormat);
		log.debug("format:" + videoFormat);

		MMediaFormat audioFormat = reader.getTrackFormat(1);
		assertNotNull(audioFormat);
		log.debug("format:" + audioFormat);

		Mp4Track videoTrack = reader.getTrack("vide");
		assertNotNull(videoTrack);
		log.debug("videoTrack:" + videoTrack);

		Mp4Atom track = videoTrack.getTrackAtom();
		log.debug("track:" + track);
		// printAtom(track);

		Mp4Track audioTrack = reader.getTrack("soun");
		assertNotNull(audioTrack);
		log.debug("audioTrack:" + audioTrack);

		track = audioTrack.getTrackAtom();
		log.debug("track:" + track);
		printAtom(track);

		Mp4Atom tkhd = track.findAtom("tkhd");
		// printAtomInfo(tkhd);

		Mp4Atom mp4a = track.findAtom("mdia.minf.stbl.stsd.mp4a");
		// printAtomInfo(mp4a);

		Mp4Atom esds = mp4a.findAtom("esds");
		// printAtomInfo(esds);

	}

	@Test
	public void testReadProperties() throws IOException {
		// Init buffer
		Mp4MemStream mp4File = new Mp4MemStream(1024 * 640);
		mp4File.writeInt32(199);
		mp4File.writeFloat(29.9f, 2);
		mp4File.write("abcd1234".getBytes());
		mp4File.buffer.position(0);

		assertEquals(0, mp4File.read());
		assertEquals(0, mp4File.read());
		assertEquals(0, mp4File.read());
		assertEquals(199, mp4File.read());

		mp4File.buffer.position(0);

		// Init Atom
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("udat");
		atom.addProperty(PT_INT, 4, "int");
		atom.addProperty(PT_FLOAT, 2, "float");
		atom.addProperty(PT_STRING, 8, "string");
		Mp4Reader.readProperties(mp4File, atom);

		// assertEquals
		assertEquals(199, atom.getPropertyInt("int"));
		assertEquals("abcd1234", atom.getPropertyString("string"));
		assertEquals(29, (int) atom.getPropertyFloat("float"));
	}

	// @Test
	public void testWriter() throws IOException {
		File file = new File("output.mp4");
		file.delete();

		int videoTrackIndex = 1;
		int audioTrackIndex = 0;

		// Source
		MMediaExtractor reader = new Mp4Reader();
		reader.setDataSource("test.mp4");

		MMediaFormat readerFormat = reader.getTrackFormat(0);
		if (MMediaTypes.VIDEO.equals(readerFormat.getMediaType())) {
			videoTrackIndex = 0;
			audioTrackIndex = 1;
		}

		MMediaFormat readerVideoFormat = reader.getTrackFormat(videoTrackIndex);
		int videoWidth = readerVideoFormat.getVideoWidth();
		int videoHeight = readerVideoFormat.getVideoHeight();

		MMediaFormat readerAudioFormat = reader.getTrackFormat(audioTrackIndex);
		int audioTimeScale = readerAudioFormat.getSampleRate();

		int videoCodec = MMediaTypes.H264;
		int audioCodec = MMediaTypes.AAC;

		log.debug("videoFormat: scale=" + videoWidth + "x" + videoHeight + "/"
				+ videoCodec);

		ByteBuffer readBuffer = ByteBuffer.allocate(videoWidth * videoHeight
				* 2);

		// Dest
		MMediaMuxer writer = new Mp4Writer("output.mp4");
		MMediaFormat videoFormat = MMediaFormat.newVideoFormat(videoCodec,
				videoWidth, videoHeight);
		writer.setVideoFormat(videoFormat);

		MMediaFormat audioFormat = MMediaFormat.newAudioFormat(audioCodec,
				audioTimeScale);
		writer.setAudioFormat(audioFormat);
		writer.start();

		// Samples
		reader.selectTrack(videoTrackIndex);

		int count = 0;
		while (true) {
			if (!reader.advance()) {
				break;
			}

			long sampleTime = reader.getSampleTime();
			int sampleFlags = reader.getSampleFlags();
			boolean isSyncSample = (sampleFlags & MMediaExtractor.FLAG_SYNC_SAMPLE) != 0;

			readBuffer.clear();
			int sampleSize = reader.readSampleData(readBuffer, 0);
			if (sampleSize <= 0) {
				break;
			}

			if (count < 10) {
				log.debug("sampleSize: " + sampleSize + "/" + readBuffer
						+ ",sync=" + isSyncSample);
			}

			MMediaBuffer mediaBuffer = new MMediaBuffer();
			mediaBuffer.setData(readBuffer);
			mediaBuffer.setSyncPoint(isSyncSample);
			mediaBuffer.setEnd(true);
			mediaBuffer.setSampleTime(sampleTime);
			writer.writeVideoData(mediaBuffer);
			count++;
		}

		log.debug("End streaming " + count);

		// Samples
		reader.selectTrack(audioTrackIndex);

		count = 0;
		while (true) {
			if (!reader.advance()) {
				break;
			}

			long sampleTime = reader.getSampleTime();

			readBuffer.clear();
			int sampleSize = reader.readSampleData(readBuffer, 0);
			if (sampleSize <= 0) {
				break;
			}

			if (count < 10) {
				log.debug("sampleSize: " + sampleTime + "/" + readBuffer);
			}

			MMediaBuffer mediaBuffer = new MMediaBuffer();
			mediaBuffer.setData(readBuffer);
			mediaBuffer.setSyncPoint(true);
			mediaBuffer.setEnd(true);
			mediaBuffer.setSampleTime(sampleTime);
			writer.writeAudioData(mediaBuffer);
			count++;
		}

		// close
		writer.stop();
		writer = null;

		reader.close();
	}

	// @Test
	public void testWriter2() throws IOException {
		File file = new File("output.mp4");
		file.delete();

		int videoTrackIndex = 1;
		int audioTrackIndex = 0;

		// Source
		MMediaExtractor reader = new Mp4Reader();
		reader.setDataSource("test.mp4");

		MMediaFormat readerFormat = reader.getTrackFormat(0);
		if (MMediaTypes.VIDEO.equals(readerFormat.getMediaType())) {
			videoTrackIndex = 0;
			audioTrackIndex = 1;
		}

		MMediaFormat readerVideoFormat = reader.getTrackFormat(videoTrackIndex);
		int videoWidth = readerVideoFormat.getVideoWidth();
		int videoHeight = readerVideoFormat.getVideoHeight();

		MMediaFormat readerAudioFormat = reader.getTrackFormat(audioTrackIndex);
		int audioTimeScale = readerAudioFormat.getSampleRate();

		int videoCodec = MMediaTypes.H264;
		int audioCodec = MMediaTypes.AAC;

		log.debug("videoFormat: scale=" + videoWidth + "x" + videoHeight + "/"
				+ videoCodec);

		ByteBuffer readBuffer = ByteBuffer.allocate(videoWidth * videoHeight
				* 2);

		// Dest
		MMediaMuxer writer = new Mp4Writer("output2.mp4");
		MMediaFormat videoFormat = MMediaFormat.newVideoFormat(videoCodec,
				videoWidth, videoHeight);
		writer.setVideoFormat(videoFormat);

		MMediaFormat audioFormat = MMediaFormat.newAudioFormat(audioCodec,
				audioTimeScale);
		writer.setAudioFormat(audioFormat);
		writer.start();

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

			if (count < 10) {
				log.debug(trackIndex + ": sampleSize: " + sampleSize + "/"
						+ readBuffer + ",sync=" + (sampleTime / 1000));
			}

			if (trackIndex == videoTrackIndex) {
				MMediaBuffer mediaBuffer = new MMediaBuffer();
				mediaBuffer.setData(readBuffer);
				mediaBuffer.setSyncPoint(isSyncSample);
				mediaBuffer.setEnd(true);
				mediaBuffer.setSampleTime(sampleTime);
				writer.writeVideoData(mediaBuffer);

			} else {
				MMediaBuffer mediaBuffer = new MMediaBuffer();
				mediaBuffer.setData(readBuffer);
				mediaBuffer.setSyncPoint(true);
				mediaBuffer.setEnd(true);
				mediaBuffer.setSampleTime(sampleTime);
				writer.writeAudioData(mediaBuffer);
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
