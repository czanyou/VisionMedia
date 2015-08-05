/***
 * The content of this file or document is CONFIDENTIAL and PROPRIETARY
 * to ChengZhen(anyou@msn.com). It is subject to the terms of a
 * License Agreement between Licensee and ChengZhen(anyou@msn.com).
 * restricting among other things, the use, reproduction, distribution
 * and transfer. Each of the embodiments, including this information and
 * any derivative work shall retain this copyright notice.
 *
 * Copyright (c) 2005-2014 ChengZhen(anyou@msn.com) All Rights Reserved.
 *
 */
package org.vision.media.mp4;

import java.io.IOException;
import java.util.List;

import org.vision.media.MMediaTypes;
import org.vision.media.avc.Mp4Video;
import org.vision.media.mp4.Mp4MediaFactory.MediaFrameInfo;

public class Mp4Moive {

	public static Mp4Track addAudioTrack(List<Mp4Track> tracks, Mp4TrackInfo trackProps) {
		if (trackProps == null) {
			return null;
		}

		int timeScale = trackProps.getSampleRate();
		long fixedSampleSize = trackProps.getFixedSampleSize();
		int type = trackProps.getCodecType();
		int channels = trackProps.getChannelCount();
		if (channels < 1) {
			channels = 1;
		}
		
		long trackId = tracks.size() + 1;
		Mp4Track track = addTrack(trackId, "soun", timeScale);
		tracks.add(track);
		
		track.setFixedSampleSize(fixedSampleSize);

		Mp4Atom atom = track.getTrackAtom();
		atom.setProperty("tkhd.volume", 1.0f);
		atom.addChildAtom(0, "mdia.minf.smhd");

		if (type == MMediaTypes.AAC) {
			atom.setProperty("mdia.minf.stbl.stsd.entryCount", 1);
			Mp4Atom mp4a = atom.addChildAtom("mdia.minf.stbl.stsd.mp4a");
			mp4a.setProperty("sampleRate", timeScale);
			mp4a.setProperty("channels", 2);

			Mp4Atom esds = mp4a.getChildAtom("esds");
			Mp4Property desc = (esds != null) ? esds.getProperty("descriptor")
					: null;
			Mp4DescriptorProperty prop = (Mp4DescriptorProperty) desc;
			if (prop != null) {
				// System.out.println("Mp4DescriptorProperty");
				Mp4Descriptor d = prop.getDescriptor();
				d.setChannelConfiguration(2);
				d.setSamplingFrequency(timeScale);
			}

		} else if (type == MMediaTypes.AMR_NB) {
			atom.setProperty("mdia.minf.stbl.stsd.entryCount", 1);
			Mp4Atom amr = atom.addChildAtom("mdia.minf.stbl.stsd.samr");
			amr.setProperty("timescale", timeScale);
			amr.setProperty("damr.modeSet", 0);
			amr.setProperty("damr.modeChangePeriod", 0);
			amr.setProperty("damr.framesPerSample", 0);

		} else if (type == MMediaTypes.PCMA) {
			atom.setProperty("mdia.minf.stbl.stsd.entryCount", 1);
			Mp4Atom alaw = atom.addChildAtom("mdia.minf.stbl.stsd.alaw");
			alaw.setProperty("sampleRate", timeScale);
			alaw.setProperty("dataReferenceIndex", 1);
			alaw.setProperty("channels", channels);
			alaw.setProperty("sampleSize", 16);
			alaw.setProperty("reserved2", 0);

		} else if (type == MMediaTypes.PCMU) {
			atom.setProperty("mdia.minf.stbl.stsd.entryCount", 1);
			Mp4Atom ulaw = atom.addChildAtom("mdia.minf.stbl.stsd.ulaw");
			ulaw.setProperty("sampleRate", timeScale);
			ulaw.setProperty("dataReferenceIndex", 1);
			ulaw.setProperty("channels", channels);
			ulaw.setProperty("sampleSize", 16);
			ulaw.setProperty("reserved2", 0);

		} else if (type == MMediaTypes.G726_16 || type == MMediaTypes.G726_24
				|| type == MMediaTypes.G726_32 || type == MMediaTypes.G726_40) {
			atom.setProperty("mdia.minf.stbl.stsd.entryCount", 1);
			Mp4Atom g726 = atom.addChildAtom("mdia.minf.stbl.stsd.g726");
			g726.setProperty("sampleRate", timeScale);
			g726.setProperty("dataReferenceIndex", 1);
			g726.setProperty("channels", channels);
			g726.setProperty("sampleSize", 16);
			g726.setProperty("reserved2", 16);
			if (type == MMediaTypes.G726_24) {
				g726.setProperty("reserved2", 24);

			} else if (type == MMediaTypes.G726_32) {
				g726.setProperty("reserved2", 32);

			} else if (type == MMediaTypes.G726_40) {
				g726.setProperty("reserved2", 40);
			}
		}

		track.init();

		return track;
	}

	/**
	 * ���һ��ָ�������͵� Track.
	 * 
	 * @param type
	 *            Ҫ��ӵ� Track ������.
	 * @param timeScale
	 *            ��� Track �� Time Scale ֵ.
	 * @return ���ش����� Track ��ʵ��.
	 * @throws IllegalStateException
	 *             ���ǰ��״̬���� INIT.
	 */
	public static Mp4Track addTrack(long trackId, String type, int timeScale) {

		long now = Mp4Factory.getMP4Timestamp();

		// ���� trak �ڵ�
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("trak");
		atom.setProperty("tkhd.creation_time", now);
		atom.setProperty("tkhd.modification_time", now);
		atom.setProperty("tkhd.track_ID", trackId);
		atom.setProperty("mdia.mdhd.creation_time", now);
		atom.setProperty("mdia.mdhd.modification_time", now);
		atom.setProperty("mdia.hdlr.handler_type", type);

		// ���� track ����
		Mp4Track track = new Mp4Track();
		track.setTrackAtom(atom);
		track.setTimeScale(timeScale);

		return track;
	}

	public static void addVideoParamSets(Mp4Track track, byte[] videoPpsSampleData,
			byte[] videoSqsSampleData) {
		Mp4Atom trak = (track == null) ? null : track.getTrackAtom();
		if (trak == null) {
			return;
		}

		Mp4Atom avcC = trak.findAtom("mdia.minf.stbl.stsd.avc1.avcC");
		if (avcC == null) {
			return;
		}

		// SQS
		if (videoSqsSampleData != null && videoSqsSampleData.length > 4) {
			byte[] sampleData = videoSqsSampleData;

			Mp4Property property = avcC.findProperty("sequenceEntries");
			Mp4SizeTableProperty sqs = (Mp4SizeTableProperty) property;

			sqs.addEntry(sampleData);
			avcC.setProperty("AVCProfileIndication", sampleData[1]);
			avcC.setProperty("profile_compatibility", sampleData[2]);
			avcC.setProperty("AVCLevelIndication", sampleData[3]);
			avcC.setProperty("numOfSequenceParameterSets", sqs.getRowCount());
		}

		// PPS
		if (videoPpsSampleData != null) {
			byte[] sampleData = videoPpsSampleData;
			Mp4Property property = avcC.findProperty("pictureEntries");
			Mp4SizeTableProperty pps = (Mp4SizeTableProperty) property;

			pps.addEntry(sampleData);
			avcC.setProperty("numOfPictureParameterSets", pps.getRowCount());
		}
	}

	public static Mp4Track addVideoTrack(List<Mp4Track> tracks, Mp4TrackInfo trackProps) {
		if (trackProps == null) {
			return null;
		}

		int timeScale = trackProps.getSampleRate();
		long fixedSampleSize = trackProps.getFixedSampleSize();
		int type = trackProps.getCodecType();
		int height = trackProps.getVideoHeight();
		int width = trackProps.getVideoWidth();

		long trackId = tracks.size() + 1;
		Mp4Track track = addTrack(trackId, "vide", timeScale);
		tracks.add(track);
		track.setFixedSampleSize(fixedSampleSize);

		Mp4Atom atom = track.getTrackAtom();
		atom.setProperty("tkhd.width", width);
		atom.setProperty("tkhd.height", height);

		atom.addChildAtom(0, "mdia.minf.vmhd"); // Add video media header
		atom.addChildAtom("mdia.minf.stbl.stss"); // Add sync time sample table

		if (type == MMediaTypes.H264) {
			// Add a 'avc1' atom for the H.264 Track
			atom.setProperty("mdia.minf.stbl.stsd.entryCount", 1);
			Mp4Atom avc1 = atom.addChildAtom("mdia.minf.stbl.stsd.avc1");
			avc1.setProperty("width", width);
			avc1.setProperty("height", height);
		}

		track.init();
		return track;
	}

	public static MediaFrameInfo parseSeqInfo(byte[] data) {
		MediaFrameInfo info = new MediaFrameInfo();
		Mp4Video.AvcSeqParams params = Mp4Video.readSeqInfo(data);
		if (params != null) {
			info.videoWidth = params.pic_width;
			info.videoHeight = params.pic_height;
		}

		return info;
	}

	public static void writeAtom(Mp4Stream file, Mp4Atom atom)
			throws IOException {
		int type = atom.getType();
		if ("root".equals(type) || "mdat".equals(type)) {
			return;
		}

		// writeHeaderBox
		long start = file.getPosition();
		atom.setStart(start);

		file.writeInt32(atom.getSize());
		file.write(Mp4Atom.getAtomType(type).getBytes());

		// writeProperties
		List<Mp4Property> properties = atom.getProperties();
		for (Mp4Property property : properties) {
			property.write(file);
		}

		// writeChildAtoms
		List<Mp4Atom> childAtoms = atom.getChildAtoms();
		for (Mp4Atom child : childAtoms) {
			writeAtom(file, child);
		}

		// writeBoxSize
		long end = file.getPosition();
		atom.setSize(end - start);

		file.seek(start);
		file.writeInt32(atom.getSize());
		file.seek(end);
	}

	/**
	 * ��ʼд�뵱ǰ Atom.
	 * 
	 * @param mp4Stream
	 * @return
	 * @throws IOException
	 */
	public static void writeAtomHeader(Mp4Stream file, Mp4Atom atom)
			throws IOException {
		int type = atom.getType();
		atom.setStart(file.getPosition());
		file.writeInt32(atom.getSize());
		file.write(Mp4Atom.getAtomType(type).getBytes());
	}

	/**
	 * ���д����
	 * 
	 * @param mp4Stream
	 * @throws IOException
	 */
	public static void writeAtomSize(Mp4Stream file, Mp4Atom atom)
			throws IOException {
		long start = atom.getStart();
		long end = file.getPosition();
		atom.setSize(end - start);

		file.seek(start);
		file.writeInt32(atom.getSize());
		file.seek(end);
	}

	/**
	 * д���������
	 * 
	 * @param file
	 *            Ҫд����ļ�
	 * @throws IOException
	 *             ��������
	 */
	public static void writeProperty(Mp4Stream file, Mp4Property property)
			throws IOException {
		Mp4PropertyType type = property.getType();
		if (type == null) {
			throw new IOException("Unknow property type.");
		}

		int size = property.getSize();

		switch (type) {
		case PT_INT:
			file.writeInt(property.getValueInt(), size);
			break;

		case PT_BITS:
			file.writeBits(property.getValueInt(), size);
			break;

		case PT_DATE:
			file.writeInt32(property.getValueInt());
			break;

		case PT_FLOAT:
			file.writeFloat(property.getFloatValue(), size);
			break;

		case PT_STRING:
		case PT_BYTES:
			byte[] data = property.getBytes();
			if (data != null) {
				file.write(data);
			}
			break;

		case PT_TABLE:
		case PT_DESCRIPTOR:
		case PT_SIZE_TABLE:
			// Ĭ��ʵ��
			property.write(file);
			break;
		}
	}

}
