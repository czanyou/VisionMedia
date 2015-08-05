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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaExtractor;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaTypes;

/**
 * 代表一个 MP4 文件阅读器.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class Mp4Reader extends MMediaExtractor {
	private static final Logger log = LoggerFactory.getLogger(Mp4Reader.class);

	/**
	 * 读取当前 Atom 的内容.
	 * 
	 * @param mp4File
	 *            MP4 源文件
	 * @throws IOException
	 *             如果发生读写错误
	 */
	public static void readAtom(Mp4Stream mp4File, Mp4Atom atom)
			throws IOException {
		if (!atom.getProperties().isEmpty()) {
			readProperties(mp4File, atom);
		}

		if (atom.isExpectChild()) {
			readChildAtoms(mp4File, atom);
		}
	}

	/**
	 * 读取当前 Atom 的所有的子 Atom 的内容.
	 * 
	 * @param mp4File
	 *            MP4 源文件
	 * @throws IOException
	 *             如果发生读写错误
	 */
	public static void readChildAtoms(Mp4Stream mp4File, Mp4Atom parent)
			throws IOException {
		long leftover = parent.getEnd() - mp4File.getPosition(); // 剩余的数据的长度

		// 读取所有的子节点
		while (leftover > 8) {
			long start = mp4File.getPosition();
			long size = mp4File.readInt32(); // ATOM 长度
			if (size < 8) {
				throw new IOException("Invalid atom size: " + size);
			}

			// ATOM 类型
			int type = (int) mp4File.readInt32();

			Mp4Atom atom = new Mp4Atom(type);
			Mp4Factory.getInstanae().initProperties(atom);
			parent.addChildAtom(atom);

			atom.setStart(start);
			atom.setSize(size);

			// 读取这个 ATOM 节点的属性和子节点
			readAtom(mp4File, atom);

			// 跳过没有读取完的数据
			long end = atom.getEnd();
			if ((end > 0) && (end != mp4File.getPosition())) {
				mp4File.seek(end);
			}
			leftover -= size;
		}
	}

	public static void readTurnProperties(Mp4Stream mp4File, Mp4Atom atom)
			throws IOException {

		int count = 0;
		List<Mp4Property> properties = atom.getProperties();
		for (Mp4Property property : properties) {
			try {
				property.read(mp4File);
			} catch (Exception e) {
				String info = "Read property: " + property.getName() + "/"
						+ property.getType() + "/" + property.getSize() + " ("
						+ atom.getTypeString() + ")";
				throw new IOException(info, e);
			}

			if ("sample_count".equals(property.getName())) {
				count = (int) property.getValueInt();
			}
		}
		
		Mp4TableProperty table = new Mp4TableProperty("entries");
		table.addColumn("size");
		table.addColumn("flags");
		table.addColumn("duration");
		table.setExpectSize(count);
		table.read(mp4File);
		atom.addProperty(table);
	}

	public static void readTfhdProperties(Mp4Stream mp4File, Mp4Atom atom)
			throws IOException {

		List<Mp4Property> properties = atom.getProperties();
		for (Mp4Property property : properties) {

			try {
				property.read(mp4File);
			} catch (Exception e) {
				String info = "Read property: " + property.getName() + "/"
						+ property.getType() + "/" + property.getSize() + " ("
						+ atom.getTypeString() + ")";
				throw new IOException(info, e);
			}

			if ("track_ID".equals(property.getName())) {
				break;
			}
		}

		Mp4PropertyType intType = Mp4PropertyType.PT_INT;

		long flags = atom.getPropertyInt("flags");
		if ((flags & Mp4Common.MOV_TFHD_BASE_DATA_OFFSET) != 0) {
			Mp4Property property = new Mp4Property(intType, 8,
					"base_data_offset");
			property.read(mp4File);
			atom.addProperty(property);
		}

		if ((flags & Mp4Common.MOV_TFHD_STSD_ID) != 0) {
			Mp4Property property = new Mp4Property(intType, 4, "index");
			property.read(mp4File);
			atom.addProperty(property);
		}

		if ((flags & Mp4Common.MOV_TFHD_DEFAULT_DURATION) != 0) {
			Mp4Property property = new Mp4Property(intType, 4,
					"sample_duration");
			property.read(mp4File);

			atom.addProperty(property);
		}

		if ((flags & Mp4Common.MOV_TFHD_DEFAULT_SIZE) != 0) {
			Mp4Property property = new Mp4Property(intType, 4, "sample_size");
			property.read(mp4File);
			atom.addProperty(property);
		}

		if ((flags & Mp4Common.MOV_TFHD_DEFAULT_FLAGS) != 0) {
			Mp4Property property = new Mp4Property(intType, 4, "sample_flags");
			property.read(mp4File);
			atom.addProperty(property);
		}

	}

	/**
	 * 读取当前 Atom 的所有的属性的值.
	 * 
	 * @param mp4File
	 *            MP4 源文件
	 * @throws IOException
	 *             如果发生读写错误
	 */
	public static void readProperties(Mp4Stream mp4File, Mp4Atom atom)
			throws IOException {
		if (atom.getType() == Mp4Atom.getAtomId("trun")) {
			readTurnProperties(mp4File, atom);
			return;

		} else if (atom.getType() == Mp4Atom.getAtomId("tfhd")) {
			readTfhdProperties(mp4File, atom);
			return;
		}

		Mp4Property lastProperty = null;

		long start = atom.getStart();
		long size = atom.getSize();

		List<Mp4Property> properties = atom.getProperties();
		for (Mp4Property property : properties) {
			Mp4PropertyType type = property.getType();
			if (type == Mp4PropertyType.PT_TABLE) {
				if (lastProperty == null) {
					break;
				}

				long count = lastProperty.getValueInt();
				property.setExpectSize(count);

			} else if (type == Mp4PropertyType.PT_SIZE_TABLE) {
				if (lastProperty == null) {
					break;
				}

				long count = lastProperty.getValueInt();
				property.setExpectSize(count);

			} else if (type == Mp4PropertyType.PT_STRING) {
				if (property.getSize() <= 0) {
					long expectSize = (start + size) - mp4File.getPosition();
					property.setSize((int) expectSize);
				}

			} else if (type == Mp4PropertyType.PT_DESCRIPTOR) {
				long expectSize = (start + size) - mp4File.getPosition();
				property.setExpectSize(expectSize);
				property.setSize((int) expectSize);
			}

			try {
				property.read(mp4File);
			} catch (Exception e) {
				String info = "Read property: " + property.getName() + "/"
						+ property.getType() + "/" + property.getSize() + " ("
						+ atom.getTypeString() + ")";
				throw new IOException(info, e);
			}
			lastProperty = property;
		}
	}

	/**
	 * 读取这个属性的值
	 * 
	 * @param mp4File
	 *            要读取的文件
	 * @throws IOException
	 *             如果发生错误
	 */
	public static void readProperty(Mp4Stream mp4File, Mp4Property property)
			throws IOException {
		Mp4PropertyType type = property.getType();
		if (type == null) {
			throw new IOException("Unknow property type.");
		}

		int size = property.getSize();

		switch (type) {
		case PT_INT:
			property.setValue(mp4File.readInt(size));
			break;

		case PT_BITS:
			property.setValue(mp4File.readBits(size));
			break;

		case PT_DATE:
			property.setValue(mp4File.readInt32());
			break;

		case PT_FLOAT:
			property.setValue(mp4File.readFloat(size));
			break;

		case PT_STRING:
		case PT_BYTES: {
			if (size > 0) {
				byte data[] = new byte[size];
				mp4File.readBytes(data);
				property.setValue(data);
			}
			break;
		}

		case PT_TABLE:
		case PT_DESCRIPTOR:
		case PT_SIZE_TABLE:
			// 默认没有实现
			break;
		}
	}

	/** 打开的 MP4 文件的名称. */
	private String fileName;

	/** Track 列表. */
	private List<Mp4TrackInfo> mediaFormats = new ArrayList<Mp4TrackInfo>();

	/** 相关的 MP4 文件. */
	private Mp4Stream mp4Stream;

	/** Track 列表. */
	private List<Mp4Track> mp4Tracks = new ArrayList<Mp4Track>();

	/** 当前 Sample. */
	private Mp4Sample nextSample;

	/** 当前位置. */
	private long position;

	/** 根 Atom 对象. */
	private Mp4Atom rootAtom;

	/** 当前选择的 Track . */
	private Mp4TrackInfo selectedTrack;

	public Mp4Reader() {

	}

	public Mp4Atom getRootAtom() {
		return rootAtom;
	}

	@Override
	public boolean advance() {
		nextSample = null;

		if (selectedTrack != null) {
			readNextSample(selectedTrack);

			Mp4Sample currentSample = selectedTrack.getCurrentSample();
			if (currentSample == null) {
				return false;
			}

			nextSample = currentSample;
			selectedTrack.setCurrentSample(null);
			return true;

		} else {

			for (Mp4TrackInfo format : mediaFormats) {
				readNextSample(format);
			}

			Mp4Sample sample = null;
			Mp4TrackInfo currentTrack = null;

			for (Mp4TrackInfo format : mediaFormats) {
				if (format == null) {
					continue;
				}

				Mp4Sample currentSample = format.getCurrentSample();
				if (currentSample == null) {
					continue;
				}

				if (sample == null) {
					sample = currentSample;
					currentTrack = format;

				} else if (sample.getSampleTime() > currentSample
						.getSampleTime()) {
					sample = currentSample;
					currentTrack = format;
				}
			}

			if (currentTrack != null) {
				sample = currentTrack.getCurrentSample();
				currentTrack.setCurrentSample(null);
			}

			if (sample == null) {
				return false;
			}

			nextSample = sample;
			return true;
		}
	}

	@Override
	public void close() {
		if (mp4Stream != null) {
			mp4Stream.close();
			mp4Stream = null;
		}

		if (rootAtom != null) {
			rootAtom.clear();
			rootAtom = null;
		}
	}

	/**
	 * 生成相关的 Track 对象.
	 */
	private void generateTracks() {
		mediaFormats.clear();

		if (rootAtom == null) {
			return;
		}

		Mp4Atom moov = rootAtom.findAtom("moov");
		if (moov == null) {
			log.debug("moov not exists!");
			return;
		}

		final int TRAK_TYPE = Mp4Atom.getAtomId("trak");
		for (Mp4Atom atom : moov.getChildAtoms()) {
			if (atom.getType() != TRAK_TYPE) {
				continue;
			}

			Mp4Track track = new Mp4Track();
			track.setTrackAtom(atom);

			Mp4TrackInfo trackInfo = new Mp4TrackInfo();
			if ("vide".equals(track.getType())) {
				trackInfo.setMediaType(MMediaTypes.VIDEO);

			} else if ("soun".equals(track.getType())) {
				trackInfo.setMediaType(MMediaTypes.AUDIO);
			}

			int trackIndex = mediaFormats.size();
			trackInfo.setTrackIndex(trackIndex);

			int codecType = 0;
			Mp4Atom trak = track.getTrackAtom();

			if (trak.findAtom("mdia.minf.stbl.stsd.avc1") != null) {
				codecType = MMediaTypes.H264;

			} else if (trak.findAtom("mdia.minf.stbl.stsd.mp4a") != null) {
				codecType = MMediaTypes.AAC;
			}

			trackInfo.setCodecType(codecType);
			trackInfo.setSampleRate(track.getTimeScale());
			trackInfo.setVideoWidth(track.getVideoWidth());
			trackInfo.setVideoHeight(track.getVideoHeight());

			mp4Tracks.add(track);
			mediaFormats.add(trackInfo);
		}
	}

	@Override
	public long getCachedDuration() {
		return 0;
	}

	@Override
	public long getDuration() {
		if (rootAtom == null) {
			return -1;
		}

		long duration = rootAtom.findProperty("moov.mvhd.duration")
				.getValueInt();
		int timeScale = getTimeScale();
		if (timeScale > 0 && timeScale != 1000) {
			duration = duration * 1000 / timeScale;
		}

		return duration;
	}

	@Override
	public long getPosition() {
		return position;
	}

	public Mp4Sample getSample() {
		return nextSample;
	}

	public ByteBuffer getSampleData() {
		return nextSample != null ? nextSample.getData() : null;
	}

	@Override
	public int getSampleFlags() {
		int flags = 0;
		if (nextSample != null) {
			if (nextSample.isSyncPoint()) {
				flags |= FLAG_SYNC_SAMPLE;
			}
		}
		return flags;
	}

	@Override
	public long getSampleTime() {
		return nextSample != null ? nextSample.getSampleTime() : 0;
	}

	@Override
	public int getSampleTrackIndex() {
		if (nextSample == null) {
			return -1;
		}

		return nextSample.getTrackIndex();
	}

	public int getTimeScale() {
		if (rootAtom != null) {
			return (int) rootAtom.findProperty("moov.mvhd.timescale")
					.getValueInt();
		}
		return -1;
	}

	/**
	 * 返回指定的类型的 Track 对象.
	 * 
	 * @param type
	 *            Track 的类型
	 * @return 指定的类型的 Track 对象.
	 */
	public Mp4Track getTrack(String type) {
		if (type == null) {
			throw new NullPointerException("Null type parameter.");
		}

		for (Mp4Track track : mp4Tracks) {
			if (type.equals(track.getType())) {
				return track;
			}
		}
		return null;
	}

	@Override
	public int getTrackCount() {
		return mediaFormats.size();
	}

	@Override
	public MMediaFormat getTrackFormat(int trackIndex) {
		if (trackIndex < 0 || trackIndex >= mediaFormats.size()) {
			return null;
		}

		return mediaFormats.get(trackIndex);
	}

	@Override
	public boolean hasCacheReachedEndOfStream() {
		return false;
	}

	/**
	 * 打开指定的 MP4 文件.
	 * 
	 * @param file
	 *            要打开的文件的路径
	 * @throws IOException
	 *             如果发生错误.
	 */
	private void open() throws IOException {
		if (mp4Stream != null) {
			return;
		}

		mp4Stream = new Mp4FileStream(fileName, true);
		// log.debug("File Name: " + fileName);

		rootAtom = new Mp4Atom(Mp4Atom.getAtomId("root"));
		rootAtom.setExpectChild(true);

		rootAtom.setSize(mp4Stream.getFileSize());
		// log.debug("File Size: " + rootAtom.getSize());

		try {
			readAtom(mp4Stream, rootAtom);
			seekTo(0, 0);
			generateTracks();
			advance();

		} catch (IOException e) {
			close();
			throw e;

		} catch (Throwable e) {
			close();
			throw new IOException(e.getMessage(), e);
		}
	}

	private void readNextSample(Mp4TrackInfo track) {
		if (track == null) {
			return;

		} else if (track.getCurrentSample() != null) {
			return;
		}

		if (track.getSampleId() <= 0) {
			track.setSampleId(1);
		}

		try {
			int index = track.getTrackIndex();
			Mp4Sample sample = readSample(index, track.getSampleId());
			track.setCurrentSample(sample);
			track.setSampleId(track.getSampleId() + 1);

		} catch (IOException e) {
		}
	}

	/**
	 * 从 MP4 文件中读取指定的 ID 的样本的属性和内容.
	 * 
	 * @param file
	 *            MP4 文件
	 * @param sampleId
	 *            要读取的 Sample 的 ID.
	 * @return 返回读取的 Sample 的实例, 如果指定 ID 的样本不存在, 则返回 null.
	 */
	public Mp4Sample readSample(int trackIndex, int sampleId)
			throws IOException {
		if (trackIndex < 0 || trackIndex >= mp4Tracks.size()) {
			return null;
		}

		Mp4Track track = mp4Tracks.get(trackIndex);
		long sampleSize = track.getSampleSize(sampleId);
		if (sampleSize <= 0) {
			return null;
		}

		Mp4Stream file = mp4Stream;

		long fileOffset = track.getSampleOffset(sampleId);
		file.seek(fileOffset);

		byte[] data = new byte[(int) sampleSize];
		file.readBytes(data);

		Mp4Sample sample = new Mp4Sample(sampleId, data);

		sample.setSyncPoint(track.isSyncPoint(sampleId));
		sample.setTrackIndex(trackIndex);
		sample.setEnd(true);

		track.getSampleTimes(sample);
		return sample;
	}

	@Override
	public int readSampleData(ByteBuffer byteBuffer, int offset) {
		if (byteBuffer == null) {
			return 0;
		}

		byteBuffer.position(offset);
		ByteBuffer sampleData = getSampleData();

		int trackIndex = getSampleTrackIndex();
		if (trackIndex < 0 || trackIndex >= mediaFormats.size()) {
			return 0;
		}

		Mp4TrackInfo track = mediaFormats.get(trackIndex);
		if (track.getCodecType() != MMediaTypes.H264) {
			int start = byteBuffer.position();
			byteBuffer.put(sampleData);
			int size = byteBuffer.position() - start;

			byteBuffer.flip();
			byteBuffer.position(offset);

			return size;
		}

		byte[] startCode = new byte[] { 0x00, 0x00, 0x00, 0x01 };
		int start = byteBuffer.position();

		int flags = getSampleFlags();
		if ((flags & FLAG_SYNC_SAMPLE) != 0) {
			Mp4Track videoTrack = mp4Tracks.get(trackIndex);

			Mp4Atom trak = videoTrack.getTrackAtom();
			Mp4Atom avcC = trak.findAtom("mdia.minf.stbl.stsd.avc1.avcC");

			Mp4SizeTableProperty sqs = (Mp4SizeTableProperty) avcC
					.findProperty("sequenceEntries");
			if (sqs != null && sqs.getRowCount() > 0) {
				ByteBuffer sqsData = ByteBuffer.wrap(sqs.getEntry(0));
				byteBuffer.put(startCode);
				byteBuffer.put(sqsData);
			}

			Mp4SizeTableProperty pps = (Mp4SizeTableProperty) avcC
					.findProperty("pictureEntries");
			if (pps != null && pps.getRowCount() > 0) {
				ByteBuffer ppsData = ByteBuffer.wrap(pps.getEntry(0));
				byteBuffer.put(startCode);
				byteBuffer.put(ppsData);
			}
		}

		while (sampleData.remaining() >= 4) {
			int size = sampleData.getInt();
			if (size <= 0) {
				break;

			} else if (size > sampleData.remaining()) {
				break;
			}

			byteBuffer.put(startCode);
			byteBuffer.put(sampleData.array(), sampleData.position(), size);

			sampleData.position(sampleData.position() + size);
		}

		int size = byteBuffer.position() - start;

		byteBuffer.flip();
		byteBuffer.position(offset);

		return size;
	}

	@Override
	public void seekTo(long time, int flags) {

	}

	@Override
	public void selectTrack(int trackIndex) {
		if (trackIndex < 0 || trackIndex >= mediaFormats.size()) {
			selectedTrack = null;
			return;
		}

		nextSample = null;

		selectedTrack = mediaFormats.get(trackIndex);
		if (selectedTrack != null) {
			selectedTrack.setSampleId(1);
			selectedTrack.setCurrentSample(null);
		}
	}

	@Override
	public void setDataSource(String file) throws IOException {
		fileName = file;
		open();
	}

	@Override
	public void unselectTrack(int index) {
		selectedTrack = null;
	}

}
