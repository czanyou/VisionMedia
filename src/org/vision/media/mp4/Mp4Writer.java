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
import org.vision.media.avc.Mp4VideoUtils;

/**
 * 代表一个 MP4 文件书写器.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class Mp4Writer extends MMediaMuxer {
	/** 书写器的状态. */
	public enum State {
		FINISHED, INIT, STARTED
	}

	private static final Logger log = LoggerFactory.getLogger(Mp4Writer.class);

	public static void fixFile(String filename) throws IOException {
		Mp4Writer writer = new Mp4Writer();
		writer.openUnfinishedStream(filename);
		writer.stop();
	}

	private Mp4TrackInfo audioTrack;

	/** 这个文件的创建时间. */
	private long creationTime;

	/** 打开的文件名. */
	private String filename;

	private Mp4Atom freeAtom;

	/** 根 Atom 对象. */
	private Mp4Atom ftypAtom;

	private Mp4IndexWriter indexWriter = new Mp4IndexWriter();

	/** 读写锁. */
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	private Mp4Atom mdatAtom;

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

	public Mp4Writer() {

	}

	/**
	 * 构建向其中写入的 MP4 文件书写器.
	 * 
	 * @param filename
	 *            MP4 文件名.
	 * @throws IOException
	 *             如果不能创建指定的文件
	 */
	public Mp4Writer(String filename) throws IOException {
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

			// start atoms
			Mp4Moive.writeAtom(mp4Stream, ftypAtom); // ftyp
			Mp4Moive.writeAtom(mp4Stream, freeAtom); // free
			Mp4Moive.writeAtomHeader(mp4Stream, mdatAtom); // mdat

			waitSyncPoint = true;
			waitParameterSets = true;

			indexWriter.writeMetaInfo(this);
			setState(State.STARTED);

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 完成这个文件, 完成写操作和收尾工作.
	 * 
	 * @throws IOException
	 *             如果发生 I/O 错误。
	 * @throws IllegalStateException
	 *             如果当前的状态不是 STARTED.
	 */
	private void endWriting() throws IOException {
		if (getState() != State.STARTED) {
			throw new IllegalStateException(
					"The state of this writer is not 'started'.");
		}

		try {
			lock.writeLock().lock();
			setState(State.FINISHED);

			Mp4Factory factory = Mp4Factory.getInstanae();
			Mp4Atom moovAtom = factory.newAtom("moov");
			finishTracks(moovAtom);

			Mp4Moive.writeAtomSize(mp4Stream, mdatAtom);

			// moov at end of the file
			moovAtom.setProperty("mvhd.creation_time", creationTime);
			moovAtom.setProperty("mvhd.modification_time", creationTime);
			moovAtom.updateSize(); // 计算 moov ATOM 需要占用的空间

			if (moovAtom.getSize() < freeAtom.getSize()) {
				mp4Stream.seek(freeAtom.getStart());
				Mp4Moive.writeAtom(mp4Stream, moovAtom);

				long size = freeAtom.getSize() - moovAtom.getSize() - 8;

				Mp4Atom freeAtom = factory.newAtom("free");
				freeAtom.addProperty(Mp4PropertyType.PT_BYTES, (int) size,
						"padding");
				Mp4Moive.writeAtom(mp4Stream, freeAtom);

			} else {
				Mp4Moive.writeAtom(mp4Stream, moovAtom);
			}

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 完成指定的 Track
	 * 
	 * @param trackProps
	 * @throws IOException
	 */
	private void finishTrack(Mp4Track track, List<Mp4Chunk> chunks)
			throws IOException {

		for (Mp4Chunk chunk : chunks) {
			track.addChunk(chunk);
		}

		track.updateTrackDuration(timeScale);
		track.finish();
	}

	/**
	 * 完成所有的 Track
	 * 
	 * @param moovAtom
	 * @return
	 * @throws IOException
	 */
	private long finishTracks(Mp4Atom moovAtom) throws IOException {

		// flush buffer
		if (videoTrack != null) {
			flushChunkBuffer(videoTrack);
		}

		if (audioTrack != null) {
			flushChunkBuffer(audioTrack);
		}

		// load index data
		try {
			indexWriter.close();
			indexWriter.loadIndexFile(filename, 2, this);

		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}

		List<Mp4Track> tracks = new ArrayList<Mp4Track>();
		long duration = 0;

		// Video track
		if (videoTrack != null) {
			Mp4Track track = Mp4Moive.addVideoTrack(tracks, videoTrack);
			Mp4Moive.addVideoParamSets(track, videoPpsSampleData,
					videoSqsSampleData);
			finishTrack(track, videoTrack.getChunks());

			duration = track.getDuration();
			long trackTimeScale = track.getTimeScale();
			if (trackTimeScale > 0) {
				duration = duration * timeScale / trackTimeScale;
			}
		}

		// Audio track
		if (audioTrack != null) {
			Mp4Track track = Mp4Moive.addAudioTrack(tracks, audioTrack);
			// log.debug(track.toString());
			finishTrack(track, audioTrack.getChunks());
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

		// last sample
		Mp4Sample lastSample = track.getCurrentSample();
		track.setCurrentSample(null);

		if (lastSample != null) {
			lastSample.setSampleDuration(40000);
			track.addSample(lastSample);
		}

		Mp4Chunk chunk = track.getCurrentChunk();
		if (chunk.isEmpty()) {
			return;
		}

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

		indexWriter.writeChunkInfo(chunk);
		chunk.clear();
	}

	/**
	 * @return the audioTrack
	 */
	public Mp4TrackInfo getAudioTrack() {
		return audioTrack;
	}

	/**
	 * @return the creationTime
	 */
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

	public State getState() {
		try {
			lock.readLock().lock();
			return state;

		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return the videoTrack
	 */
	public Mp4TrackInfo getVideoTrack() {
		return videoTrack;
	}

	public long innerWriteAudioData(MMediaBuffer sample) throws IOException {
		if (getState() != State.STARTED) {
			throw new IllegalStateException(
					"The state of this writer is not 'started'.");
		}

		if (sample == null) {
			return 0;

		} else if (waitSyncPoint) {
			return 0;
		}

		ByteBuffer data = sample.getData();
		if (data == null) {
			return 0;
		}

		// log.debug("writeAudioData: " + waitSyncPoint + "/" + data + "/"
		// + sample.getSampleTime());

		// codec
		int position = data.position();
		int size = data.limit() - data.position();
		int type = audioTrack.getCodecType();
		if (type == MMediaTypes.AAC) {
			// 去除 AAC AU 头 (4个字节)
			if ((size > 4) && (data.array()[position] == (byte) 0x00)) {
				data.position(position + 4);
			}

		} else if (type == MMediaTypes.AMR_NB) {
			if (size > 1 && data.array()[position] == (byte) 0xf0) {
				data.position(position + 1);
			}
		}

		long sampleTime = sample.getSampleTime();

		Mp4Sample mp4Sample = audioTrack.getCurrentSample();
		if (mp4Sample != null && mp4Sample.isEnd()) {
			// 写入文件
			if (mp4Sample.getSampleSize() > 0) {
				long duration = sampleTime - mp4Sample.getSampleTime();

				mp4Sample.setSampleDuration(duration);
				audioTrack.addSample(mp4Sample);
			}

			mp4Sample = null;
			audioTrack.setCurrentSample(null);

			if (audioTrack.isChunkFull()) {
				flushChunkBuffer(audioTrack);
			}
		}

		try {
			lock.writeLock().lock();
			mp4Sample = new Mp4Sample();
			mp4Sample.addData(sample.getData());
			mp4Sample.setSampleSize(sample.getSize());
			mp4Sample.setSyncPoint(true);
			mp4Sample.setEnd(true);
			mp4Sample.setSampleTime(sample.getSampleTime());
			audioTrack.setCurrentSample(mp4Sample);

		} finally {
			lock.writeLock().unlock();
		}
		return 0;
	}

	public long innerWriteVideoData(MMediaBuffer mediaBuffer)
			throws IOException {
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
				skip = true;
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

	/** 指定这个书写器是否是打开的. */
	public boolean isOpen() {
		return (mp4Stream != null) && mp4Stream.isOpen();
	}

	/**
	 * 创建指定的名称的 MP4 文件.
	 * 
	 * @param filename
	 *            要创建的文件的名称.
	 * @return 返回创建的 MP4 文件
	 * @throws IOException
	 *             如果不能创建指定的文件
	 */
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

		// Media index File
		String indxFile = filename + ".mif";
		file = new File(indxFile);
		if (file.exists()) {
			file.delete();
		}

		indexWriter.open(indxFile);

		// Atoms
		Mp4Factory factory = Mp4Factory.getInstanae();
		ftypAtom = factory.newAtom("ftyp");
		mdatAtom = factory.newAtom("mdat");
		freeAtom = factory.newAtom("free");
		creationTime = Mp4Factory.getMP4Timestamp();

		freeAtom.addProperty(Mp4PropertyType.PT_INT, 4, "size");
		freeAtom.addProperty(Mp4PropertyType.PT_BYTES, 64, "padding");
		freeAtom.setProperty("size", 64);

		//
		setState(State.INIT);
		return stream;
	}

	/**
	 * 重新打开上次没有完成的任务
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public Mp4Stream openUnfinishedStream(String filename) throws IOException {
		if (mp4Stream != null) {
			throw new IOException("The file has already been opened.");
		}

		File file = new File(filename + ".mif");
		if (!file.exists()) {
			return null; // 索引文件不存在
		}

		file = new File(filename + ".mdf");
		if (!file.exists()) {
			return null; // 数据文件不存在
		}

		log.debug("openUnfinishedStream: " + file.getAbsolutePath());
		Mp4Stream stream = new Mp4FileStream(file, false);

		Mp4Factory factory = Mp4Factory.getInstanae();
		ftypAtom = factory.newAtomWithoutInit("ftyp");
		mdatAtom = factory.newAtomWithoutInit("mdat");
		creationTime = Mp4Factory.getMP4Timestamp();
		this.filename = filename;

		// scan atoms
		while (true) {
			long start = stream.getPosition();
			try {
				long size = stream.readInt32();
				long type = stream.readInt32();

				if (type == Mp4Atom.getAtomId("ftyp")) {
					Mp4Reader.readAtom(stream, ftypAtom);
					ftypAtom.setStart(start);
					ftypAtom.setSize(size);

				} else if (type == Mp4Atom.getAtomId("mdat")) {
					mdatAtom.setStart(start);
					mdatAtom.setSize(size);
				}

				if (size < 8) {
					break;
				}

				stream.seek(start + size);

			} catch (Exception e) {
				break;
			}
		}

		log.debug(mdatAtom.getStart() + "");
		if (mdatAtom.getStart() <= 0) {
			return null;
		}

		// fix mdat atom size
		stream.seek(stream.getFileSize());
		Mp4Moive.writeAtomSize(stream, mdatAtom);
		setState(State.INIT);

		// load moov data
		indexWriter.loadIndexFile(filename, 0, this);
		setState(State.STARTED);

		// fix moov atom
		mp4Stream = stream;
		return stream;
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

		if (indexWriter != null) {
			indexWriter.close();
		}

		if (mp4Stream != null) {
			mp4Stream.close();
			mp4Stream = null;
		}

		if (getState() == State.FINISHED) {
			// Media data file
			File mdfFile = new File(filename + ".mdf");
			File mp4File = new File(filename);
			if (mp4File.exists()) {
				mp4File.delete();
			}

			boolean ret = mdfFile.renameTo(mp4File);
			if (mp4File.exists()) {
				// Media index file
				File indexFile = new File(filename + ".mif");
				indexFile.delete();
			}

			if (!ret) {
				log.debug("rename '" + mdfFile.getAbsolutePath() + "' to '"
						+ mp4File.getAbsolutePath() + "' (" + ret + ").");
			}
		}

		videoSqsSampleData = null;
		videoPpsSampleData = null;

		videoTrack = null;
		audioTrack = null;

		ftypAtom = null;
		mdatAtom = null;

		setState(null);
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

		int type = Mp4VideoUtils.getNaluType(sampleData.array(),
				sampleData.position());
		if (!waitParameterSets) {
			if (type == Mp4VideoUtils.H264_NAL_TYPE_SEQ_PARAM) {
				return 1;

			} else if (type == Mp4VideoUtils.H264_NAL_TYPE_PIC_PARAM) {
				return 1;
			}

			return 0;
		}

		if (type == Mp4VideoUtils.H264_NAL_TYPE_SEQ_PARAM) {
			if (videoSqsSampleData == null) {
				byte[] data = new byte[sampleSize];
				sampleData.get(data);
				videoSqsSampleData = data;
			}

			return 1;

		} else if (type == Mp4VideoUtils.H264_NAL_TYPE_PIC_PARAM) {
			if (videoPpsSampleData == null) {
				byte[] data = new byte[sampleSize];
				sampleData.get(data);
				videoPpsSampleData = data;
			}

			return 1;

		} else {
			if (videoPpsSampleData == null || videoSqsSampleData == null) {
				return 0;
			}

			waitParameterSets = false;
			indexWriter.writeParameterSets(videoSqsSampleData,
					videoPpsSampleData);
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

	/**
	 * @param creationTime
	 *            the creationTime to set
	 */
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public void setParameterSet(byte[] sqsSampleData, byte[] ppsSampleData) {
		videoSqsSampleData = sqsSampleData;
		videoPpsSampleData = ppsSampleData;
	}

	/** 设置这个书写器的状态. */
	protected void setState(State state) {
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
		videoTrack.setSampleCountPerChunk(25);
		videoTrack.setDurationPerChunk(1000000);
		return 0;
	}

	private long writeAudioData(MMediaBuffer sample, int flags) {
		try {
			return innerWriteAudioData(sample);
		} catch (IOException e) {
			return -1;
		}
	}

	public long writeAudioData(MMediaBuffer sampleInfo) {

		ByteBuffer readBuffer = sampleInfo.getData();
		int size = readBuffer.limit() - readBuffer.position();
		// log.debug("writeAudioData: " + size + "/" + readBuffer);

		if (size <= 0) {
			return 0;
		}
		
		ByteBuffer data = ByteBuffer.allocate(size);
		data.put(readBuffer);
		data.flip();

		// log.debug("writeAudioData: " + size + "/" + data + "/" +
		// sampleInfo.getSampleTime());
		sampleInfo.setData(data);
		return writeAudioData(sampleInfo, 0);
	}

	private long writeVideoData(MMediaBuffer mediaBuffer, int flags) {
		try {
			return innerWriteVideoData(mediaBuffer);
		} catch (IOException e) {
			return -1;
		}
	}

	public long writeVideoData(MMediaBuffer sampleInfo) {
		ByteBuffer readBuffer = sampleInfo.getData();
		byte[] startCode = new byte[] { 0x00, 0x00, 0x00, 0x01 };
		
		boolean isSyncSample = sampleInfo.isSyncPoint();
		long sampleTime = sampleInfo.getSampleTime();
		
		int headerSize = getStartCodeLength(readBuffer);
		// log.debug("headerSize: " + headerSize + "/size=" + sampleInfo.getSize());
		if (headerSize == 3) {
			startCode = new byte[] { 0x00, 0x00, 0x01 };
		}
		
		boolean isEnd = false;
		while (!isEnd) {
			int pos = Mp4Utils.byteIndexOf(readBuffer, startCode, 1);
			int size = readBuffer.limit() - readBuffer.position() - 4;
			if (pos > 0) {
				size = pos - readBuffer.position() - 4;

			} else {
				isEnd = true;
			}

			//log.debug("count: size=" + size + "/pos=" + pos + "/" + readBuffer);

			byte[] bytes = new byte[size];
			readBuffer.getInt();
			readBuffer.get(bytes);
			if (readBuffer.limit() - readBuffer.position() <= 0) {
				isEnd = true;
			}

			MMediaBuffer mediaBuffer = new MMediaBuffer();
			mediaBuffer.setData(ByteBuffer.wrap(bytes));
			mediaBuffer.setSyncPoint(isSyncSample);
			mediaBuffer.setEnd(isEnd);
			mediaBuffer.setSampleTime(sampleTime);
			writeVideoData(mediaBuffer, 0);

			isSyncSample = false;
		}

		return 0;
	}
}
