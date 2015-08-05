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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaMuxer;
import org.vision.media.MMediaTypes;
import org.vision.media.avc.Mp4Video;
import org.vision.media.mp4.Mp4Writer.State;

/**
 * 以分片的方式写 MP4 文件
 * 
 * @author ChengZhen(anyou@msn.com)
 *
 */
public class Mp4StreamWriter extends MMediaMuxer {

	private static final Logger log = LoggerFactory
			.getLogger(Mp4StreamWriter.class);

	private Mp4TrackInfo audioTrack;

	/** 这个文件的创建时间. */
	private long creationTime;

	/** 打开的文件名. */
	private String filename;

	/** 根 Atom 对象. */
	private Mp4Atom ftypAtom;

	/** 指出是否已经生成了 moov ATOM. */
	private boolean isMoovWrite = false;

	/** 读写锁. */
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	private Mp4Atom mdatAtom;

	private Mp4Atom moovAtom;

	/** 相关的 MP4 文件. */
	private Mp4Stream mp4Stream;

	/** 当前状态. */
	private State state;

	private long timeScale = 1000;

	private byte[] videoPpsSampleData;

	private byte[] videoSqsSampleData;

	private Mp4TrackInfo videoTrack;

	private boolean waitParameterSets = true;

	private boolean waitSyncPoint = true;

	private int frameRate = 15;

	private int chunkCount = 0;

	public Mp4StreamWriter(String filename) throws IOException {
		mp4Stream = openStream(filename);
	}

	@Override
	public void start() throws IOException {
		if (getState() != State.INIT) {
			throw new IllegalStateException(
					"The state of this writer is not 'init'.");
		}

		// 写入文件头, 并准备开始写媒体数据
		try {
			lock.writeLock().lock();

			ftypAtom.getProperty("compatible_brands").setSize(12);
			ftypAtom.setProperty("major_brand", "dash");
			ftypAtom.setProperty("compatible_brands", "iso6avc1mp41");

			// moov at end of the file
			moovAtom.addChildAtom("mvex");
			moovAtom.setProperty("mvex.trex.track_ID", 1);
			moovAtom.setProperty("mvex.trex.default_sample_description_index",
					1);

			moovAtom.setProperty("mvhd.creation_time", creationTime);
			moovAtom.setProperty("mvhd.modification_time", creationTime);
			moovAtom.updateSize(); // 计算 moov ATOM 需要占用的空间

			// start atoms
			Mp4Moive.writeAtom(mp4Stream, ftypAtom); // ftyp
			// Mp4Moive.writeAtom(mp4Stream, moovAtom); // moov

			waitSyncPoint = true;
			waitParameterSets = true;

			setState(State.STARTED);

		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void stop() {
		if (getState() == State.STARTED) {
			try {
				endWriting();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}

		if (mp4Stream != null) {
			mp4Stream.close();
			mp4Stream = null;
		}

		if (getState() == State.FINISHED) {

			// Media data file
			File mdfFile = new File(filename + ".mdf");
			File mp4File = new File(filename);

			boolean ret = mdfFile.renameTo(mp4File);
			if (mp4File.exists()) {
				// Media index file
				File indexFile = new File(filename + ".mif");
				indexFile.delete();
			}

			log.debug("rename '" + mdfFile.getAbsolutePath() + "' to '"
					+ mp4File.getAbsolutePath() + "' (" + ret + ").");
		}

		videoSqsSampleData = null;
		videoPpsSampleData = null;

		videoTrack = null;
		audioTrack = null;

		ftypAtom = null;
		mdatAtom = null;

		setState(null);
	}

	private void endWriting() throws IOException {
		if (getState() != State.STARTED) {
			throw new IllegalStateException(
					"The state of this writer is not 'started'.");
		}

		try {
			lock.writeLock().lock();
			setState(State.FINISHED);

			Mp4Moive.writeAtomSize(mp4Stream, mdatAtom);

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 完成所有的 Track
	 * 
	 * @param moovAtom
	 * @return
	 * @throws IOException
	 */
	private long finishTracks(Mp4Atom moovAtom) {

		List<Mp4Track> tracks = new ArrayList<Mp4Track>();
		long duration = 0;

		// Video track
		if (videoTrack != null) {
			Mp4Track track = Mp4Moive.addVideoTrack(tracks, videoTrack);
			Mp4Moive.addVideoParamSets(track, videoPpsSampleData,
					videoSqsSampleData);

			log.debug("finishTracks:" + videoPpsSampleData);

			duration = track.getDuration();
			long trackTimeScale = track.getTimeScale();
			if (trackTimeScale > 0) {
				duration = duration * timeScale / trackTimeScale;
			}
		}

		// The moov atom
		for (Mp4Track track : tracks) {
			moovAtom.addChildAtom(track.getTrackAtom());
		}

		moovAtom.setProperty("mvhd.next_track_ID", tracks.size() + 1);
		moovAtom.setProperty("mvhd.duration", duration);
		moovAtom.setProperty("mvhd.timescale", timeScale);
		return duration;
	}

	/**
	 * 把当前 Chunk 的内容写到文件中
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void flushChunkBuffer(Mp4TrackInfo track) throws IOException {
		if (track == null) {
			return;
		}

		Mp4Chunk chunk = track.getCurrentChunk();
		if (chunk == null) {
			return;
		}

		long duration = chunk.getAvgDuration();

		// last sample
		Mp4Sample lastSample = track.getCurrentSample();
		track.setCurrentSample(null);

		if (lastSample != null) {
			lastSample.setSampleDuration(duration);
			track.addSample(lastSample);
		}

		if (chunk.isEmpty()) {
			return;
		}

		chunkCount++;
		chunk.setChunkId(chunkCount);

		int trackId = track.getTrackIndex() + 1;
		long timescale = track.getSampleRate();
		duration = duration * timescale / 1000000;
		writeChunkHeader(trackId, duration, chunk);

		// Chunk data
		long chunkOffset = mp4Stream.getPosition();
		for (Mp4Sample sample : chunk.getSamples()) {
			List<ByteBuffer> datas = sample.getBuffers();
			if (datas == null) {
				continue;
			}

			for (ByteBuffer data : datas) {
				mp4Stream.write(data);
			}
		}

		chunk.setChunkOffset(chunkOffset);
		track.addChunk(chunkOffset);

		chunk.clear();

		Mp4Moive.writeAtomSize(mp4Stream, mdatAtom);
	}

	public long getCreationTime() {
		return creationTime;
	}

	public long getDuration() {
		return (videoTrack == null) ? 0 : videoTrack.getDuration();
	}

	private int getStartCodeLength(ByteBuffer sampleData) {
		int headerSize = 0;

		int pos = sampleData.position();
		if (sampleData.get(pos) != 0x00) {
			return 0;

		} else if (sampleData.get(pos + 1) != 0x00) {
			return 0;

		} else {
			headerSize = (sampleData.get(pos + 2) == 0x01) ? 3 : 4;
		}

		return headerSize;
	}

	private State getState() {
		try {
			lock.readLock().lock();
			return state;

		} finally {
			lock.readLock().unlock();
		}
	}

	private Mp4Stream openStream(String filename) throws IOException {
		if (mp4Stream != null) {
			throw new IOException("The file has already been opened.");
		}

		this.filename = filename;

		// Media data file
		String mdatFile = filename + ".mdf";
		File file = new File(mdatFile);
		if (file.exists()) {
			file.delete();
		}
		Mp4Stream stream = new Mp4FileStream(mdatFile, false);

		// Atoms
		Mp4Factory factory = Mp4Factory.getInstanae();
		ftypAtom = factory.newAtom("ftyp");
		moovAtom = factory.newAtom("moov");
		mdatAtom = factory.newAtom("mdat");

		creationTime = Mp4Factory.getMP4Timestamp();

		//
		setState(State.INIT);
		return stream;
	}

	/**
	 * 提取 H.264 的序列, 图像等参数集数据.
	 * 
	 * @param track
	 *            相关的 Track 对象.
	 * @param sampleData
	 *            样本内容.
	 */
	private long saveVideoParamSets(ByteBuffer sampleData) {
		if (sampleData == null) {
			return 0;
		}

		int sampleSize = sampleData.limit() - sampleData.position();
		if (sampleSize < 4) {
			return 0;
		}

		int type = Mp4Video.getNaluType(sampleData.array(),
				sampleData.position());
		if (!waitParameterSets) {
			if (type == Mp4Video.H264_NAL_TYPE_SEQ_PARAM) {
				return 1;

			} else if (type == Mp4Video.H264_NAL_TYPE_PIC_PARAM) {
				return 1;
			}

			return 0;
		}

		if (type == Mp4Video.H264_NAL_TYPE_SEQ_PARAM) {
			if (videoSqsSampleData == null) {
				byte[] data = new byte[sampleSize];
				sampleData.get(data);
				videoSqsSampleData = data;

				log.debug("videoSqsSampleData:" + videoSqsSampleData);
			}

			return 1;

		} else if (type == Mp4Video.H264_NAL_TYPE_PIC_PARAM) {
			if (videoPpsSampleData == null) {
				byte[] data = new byte[sampleSize];
				sampleData.get(data);
				videoPpsSampleData = data;

				log.debug("videoPpsSampleData:" + videoPpsSampleData);
			}

			return 1;

		} else {
			if (videoPpsSampleData == null || videoSqsSampleData == null) {
				return 0;
			}

			waitParameterSets = false;
		}

		return 0;
	}

	@Override
	public int setAudioFormat(MMediaFormat mediaFormat) {
		if (audioTrack == null) {
			audioTrack = new Mp4TrackInfo();
		}

		audioTrack.setSampleRate(mediaFormat.getSampleRate());
		audioTrack.setCodecType(mediaFormat.getCodecType());
		audioTrack.setMediaType("audio");
		audioTrack.setFixedSampleSize(0);
		audioTrack.setSampleCountPerChunk(50);
		audioTrack.setDurationPerChunk(1000000);
		return 0;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	private void setState(State state) {
		this.state = state;
	}

	@Override
	public int setVideoFormat(MMediaFormat mediaFormat) {
		if (videoTrack == null) {
			videoTrack = new Mp4TrackInfo();
		}

		videoTrack.setSampleRate(mediaFormat.getSampleRate());
		videoTrack.setVideoWidth(mediaFormat.getVideoWidth());
		videoTrack.setVideoHeight(mediaFormat.getVideoHeight());
		videoTrack.setCodecType(mediaFormat.getCodecType());
		videoTrack.setMediaType("video");
		videoTrack.setFixedSampleSize(0);
		videoTrack.setSampleCountPerChunk(frameRate * 30);
		videoTrack.setDurationPerChunk(1000 * 1000 * 30);
		return 0;
	}

	@Override
	public long writeAudioData(MMediaBuffer mediaBuffer) {
		return 0;
	}

	private void writeChunkHeader(int trackId, long duration, Mp4Chunk chunk)
			throws IOException {
		if (!isMoovWrite) {
			isMoovWrite = true;

			finishTracks(moovAtom);
			Mp4Moive.writeAtom(mp4Stream, moovAtom); // moov
		}

		Mp4Factory factory = Mp4Factory.getInstanae();
		Mp4Atom moofAtom = factory.newAtom("moof");
		moofAtom.setProperty("mfhd.sequence_number", chunk.getChunkId());

		Mp4Atom tfhdAtom = moofAtom.findAtom("traf.tfhd");

		Mp4PropertyType intType = Mp4PropertyType.PT_INT;
		tfhdAtom.addProperty(intType, 4, "sample_duration");

		tfhdAtom.setProperty("flags", Mp4Common.MOV_TFHD_DEFAULT_DURATION);
		tfhdAtom.setProperty("track_ID", trackId);
		tfhdAtom.setProperty("sample_duration", duration);

		Mp4Atom trunAtom = moofAtom.findAtom("traf.trun");
		trunAtom.setProperty("sample_count", chunk.getSampleCount());
		trunAtom.setProperty("flags", 0xE01);

		Mp4TableProperty table = new Mp4TableProperty("entries");
		table.addColumn("size");
		table.addColumn("flags");
		table.addColumn("duration");

		for (Mp4Sample sample : chunk.getSamples()) {
			int flags = 65536;
			if (sample.isSyncPoint()) {
				flags = 0;
			}

			long size = sample.getSampleSize();
			table.addRow(size, flags, 0);
		}

		trunAtom.addProperty(table);

		moofAtom.updateSize();
		trunAtom.setProperty("data_offset", moofAtom.getSize() + 8);

		Mp4Moive.writeAtom(mp4Stream, moofAtom); // moof
		Mp4Moive.writeAtomHeader(mp4Stream, mdatAtom); // mdat
	}

	public long writeVideoNalu(MMediaBuffer mediaBuffer) throws IOException {
		if (getState() != State.STARTED) {
			throw new IllegalStateException(
					"The state of this writer is not 'started'.");
		}

		if (mediaBuffer == null) {
			log.debug("mediaBuffer: " + mediaBuffer);
			return 0;
		}

		if (waitSyncPoint) {
			if (!mediaBuffer.isSyncPoint()) {
				log.debug("mediaBuffer: " + mediaBuffer);
				return 0;
			}

			waitSyncPoint = false;
		}

		boolean skip = false;

		// codec
		if (videoTrack.getCodecType() == MMediaTypes.H264) {
			ByteBuffer sampleData = mediaBuffer.getData();
			if ((sampleData == null) || (sampleData.limit() <= 0)) {
				return 0;
			}

			// 跳过 H.264 同步码, 一般为 "00 00 01" 或 "00 00 00 01"
			int pos = sampleData.position();
			int headerSize = getStartCodeLength(sampleData);
			sampleData.position(pos + headerSize);

			// 提取序列, 图像等参数集
			pos = sampleData.position();
			long ret = saveVideoParamSets(sampleData);
			sampleData.position(pos);

			if (ret > 0) {
				// skip = true; // FIXME: test only
			}
		}

		try {
			lock.writeLock().lock();

			long sampleTime = mediaBuffer.getSampleTime();
			boolean isSyncSample = mediaBuffer.isSyncPoint();

			Mp4Sample mp4Sample = videoTrack.getCurrentSample();
			if (mp4Sample != null && mp4Sample.isEnd()) {
				// 写入文件
				if (mp4Sample.getSampleSize() > 0) {
					long duration = sampleTime - mp4Sample.getSampleTime();
					mp4Sample.setSampleDuration(duration);
					videoTrack.addSample(mp4Sample);
				}

				mp4Sample = null;
				videoTrack.setCurrentSample(null);

				if (videoTrack.isChunkFull()) {
					// log.debug("isChunkFull: " + sampleTime);
					flushChunkBuffer(videoTrack);
				}
			}

			if (mp4Sample == null) {
				mp4Sample = new Mp4Sample();
				mp4Sample.setSampleTime(sampleTime);
				mp4Sample.setSyncPoint(isSyncSample);
				videoTrack.setCurrentSample(mp4Sample);
			}

			if (!skip) {
				mp4Sample.addDataAndHeader(mediaBuffer.getData());
			}

			if (mediaBuffer.isEnd()) {
				mp4Sample.setEnd(true);
			}

		} finally {
			lock.writeLock().unlock();
		}
		return 0;
	}

	@Override
	public long writeVideoData(MMediaBuffer sampleInfo) {
		try {
			return innerWriteVideoData(sampleInfo);
			
		} catch (IOException e) {
			return -1;
		}
	}

	public long innerWriteVideoData(MMediaBuffer sampleInfo) throws IOException {
		ByteBuffer readBuffer = sampleInfo.getData();

		boolean isEnd = false;
		boolean isSyncSample = sampleInfo.isSyncPoint();
		if (isSyncSample) {
			flushChunkBuffer(videoTrack);
		}

		long sampleTime = sampleInfo.getSampleTime();
		while (!isEnd) {
			// 解析 NALU
			int pos = Mp4Utils.byteIndexOf(readBuffer, Mp4Common.START_CODE, 1);
			int size = 0;
			if (pos > 0) {
				size = pos - readBuffer.position() - 4;

			} else {
				size = readBuffer.limit() - readBuffer.position() - 4;
				isEnd = true;
			}

			if (size <= 0) {
				break;
			}

			// log.debug("count: " + size + "/" + pos + "/" + readBuffer);
			// data
			byte[] bytes = new byte[size];
			readBuffer.getInt();
			readBuffer.get(bytes);

			// 写入一个 NALU 单元
			MMediaBuffer mediaBuffer = new MMediaBuffer();
			mediaBuffer.setData(ByteBuffer.wrap(bytes));
			mediaBuffer.setSyncPoint(isSyncSample);
			mediaBuffer.setEnd(isEnd);
			mediaBuffer.setSampleTime(sampleTime);
			writeVideoNalu(mediaBuffer);

			// reset flags
			isSyncSample = false;
		}

		return 0;
	}

}
