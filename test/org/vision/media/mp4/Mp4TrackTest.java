package org.vision.media.mp4;

import java.io.IOException;

import org.junit.Assert;

import org.junit.Test;

public class Mp4TrackTest {

	@Test public void testTrack() throws IOException {
		try {
			Mp4Track track = new Mp4Track();
			track.setTrackAtom(null);
			Assert.fail("Want NullPointerException");
		} catch (NullPointerException e) {
			System.out.println(e.getClass());
		}

		try {
			Mp4Atom atom = Mp4Factory.getInstanae().newAtom("mdat");
			Mp4Track track = new Mp4Track();
			track.setTrackAtom(atom);
			Assert.fail("Want IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getClass());
		}

		Mp4Atom trak = Mp4Factory.getInstanae().newAtom("trak");
		trak.addChildAtom("mdia.minf.stbl.stss");

		Mp4Track track = new Mp4Track();
		track.setTrackAtom(trak);
		Assert.assertEquals(trak, track.getTrackAtom());

		track.setTrackId(2);
		Assert.assertEquals(2, track.getTrackId());

		track.setType("vide");
		Assert.assertEquals("vide", track.getType());

		track.setDuration(1001);
		Assert.assertEquals(1001, track.getDuration());

		track.setTimeScale(1000);
		Assert.assertEquals(1000, track.getTimeScale());

		/////////////////////////////////////////////

		Mp4Chunk chunk = new Mp4Chunk();
		Mp4Sample sample = new Mp4Sample();
		sample.setSampleSize(1024);
		sample.setSampleDuration(100);
		sample.setSampleTime(0);
		sample.setSyncPoint(true);
		sample.setSampleId(1);
		
		chunk.addSample(sample);
		track.addSample(sample);

		int chunkSamples = chunk.getSampleCount();
		long chunkDuration = chunk.getDuration();
		int chunkBufferSize = chunk.getSize();

		Assert.assertEquals(1, chunkSamples);
		Assert.assertEquals(100, chunkDuration);
		Assert.assertEquals(1024, chunkBufferSize);
		
		
	
		Mp4TableProperty sizeTable = (Mp4TableProperty)trak.findProperty("mdia.minf.stbl.stsz.entries");
		Assert.assertEquals(1, sizeTable.getRowCount());
		Assert.assertEquals(1024, sizeTable.getValue(0, 0));

		Mp4TableProperty syncTable	= (Mp4TableProperty)trak.findProperty("mdia.minf.stbl.stss.entries");
		//print("", syncTable);
		Assert.assertEquals(1, syncTable.getRowCount());
		Assert.assertEquals(1, syncTable.getValue(0, 0));

		/////////////////////////////////////////////

		sample = new Mp4Sample();
		sample.setSampleSize(512);
		sample.setSampleDuration(100);
		sample.setSampleTime(0);
		sample.setSyncPoint(false);
		sample.setSampleId(2);
		
		chunk.addSample(sample);
		track.addSample(sample);

		Mp4TableProperty timeTable 	= (Mp4TableProperty)trak.findProperty("mdia.minf.stbl.stts.entries");
		Assert.assertEquals(1, timeTable.getRowCount());
		Assert.assertEquals(100, timeTable.getValue(0, 1));

		Assert.assertEquals(1, syncTable.getRowCount());
		Assert.assertEquals(1, syncTable.getValue(0, 0));

		Assert.assertEquals(2, sizeTable.getRowCount());
		Assert.assertEquals(512, sizeTable.getValue(1, 0));

		chunkSamples = chunk.getSampleCount();
		chunkDuration = chunk.getDuration();
		chunkBufferSize = chunk.getSize();
		
		Assert.assertEquals(2, chunkSamples);
		Assert.assertEquals(200, chunkDuration);
		Assert.assertEquals(1536, chunkBufferSize);

		/////////////////////////////////////////////

		sample = new Mp4Sample();
		sample.setSampleSize(512);
		sample.setSampleDuration(900);
		sample.setSampleTime(0);
		sample.setSyncPoint(false);
		sample.setSampleId(3);
		
		chunk.addSample(sample);
		track.addSample(sample);

		chunkSamples = chunk.getSampleCount();
		chunkDuration = chunk.getDuration();
		chunkBufferSize = chunk.getSize();

		Assert.assertEquals(3, chunkSamples);
		Assert.assertEquals(1100, chunkDuration);
		Assert.assertEquals(2048, chunkBufferSize);
	}

}
