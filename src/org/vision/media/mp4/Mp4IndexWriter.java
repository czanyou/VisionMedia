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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaFormat;
import org.vision.media.MMediaTypes;

public class Mp4IndexWriter {

	private static final Logger log = LoggerFactory
			.getLogger(Mp4IndexWriter.class);

	private FileWriter indexWriter;

	public void close() {
		if (indexWriter != null) {
			try {
				indexWriter.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
			indexWriter = null;
		}
	}

	/**
	 * 
	 * @param filename
	 * @param flags
	 * @throws IOException
	 */
	public void loadIndexFile(String filename, int flags, Mp4Writer writer)
			throws IOException {
		FileReader fileReader = new FileReader(filename + ".mif");
		BufferedReader reader = new BufferedReader(fileReader);

		Mp4Chunk currentChunk = null;
		Mp4TrackInfo videoTrack = writer.getVideoTrack();
		Mp4TrackInfo audioTrack = writer.getAudioTrack();

		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}

				// MP4 sample
				if (line.startsWith("#") || line.startsWith("@")) {
					String tokens[] = line.split(",");
					if (currentChunk == null || tokens.length < 3) {
						continue;
					}

					Mp4Sample sample = new Mp4Sample();
					sample.setSampleId(Mp4Utils.parseInt(tokens[0].substring(1)));
					sample.setSampleSize(Mp4Utils.parseLong(tokens[1]));
					sample.setSampleDuration(Mp4Utils.parseLong(tokens[2]));
					sample.setSyncPoint(line.startsWith("@"));
					currentChunk.addSample(sample);

					// log.debug("" + sample.getSampleId() + "/" +
					// sample.getSampleSize());

					// MP4 chunk
				} else if (line.startsWith("$chunk,")) {
					String tokens[] = line.split(",");
					if (tokens.length <= 4) {
						continue;
					}

					if (currentChunk != null) {

						if (currentChunk.getPayload() == MMediaTypes.VIDEO_TYPE) {
							if (videoTrack != null) {
								videoTrack.addChunk(currentChunk);
							}

						} else {
							if (audioTrack != null) {
								audioTrack.addChunk(currentChunk);
							}
						}

						currentChunk = null;
					}

					currentChunk = new Mp4Chunk();
					currentChunk.setPayload(Mp4Utils.parseInt(tokens[1]));
					currentChunk.setChunkId(Mp4Utils.parseInt(tokens[2]));
					currentChunk.setChunkOffset(Mp4Utils.parseLong(tokens[4]));

				} else {
					if (flags == 0) {
						parseMetaInfo(line, writer);
					}
				}
			}

			if (currentChunk != null) {
				if (currentChunk.getPayload() == MMediaTypes.VIDEO_TYPE) {
					if (videoTrack != null) {
						videoTrack.addChunk(currentChunk);
					}

				} else {
					if (audioTrack != null) {
						audioTrack.addChunk(currentChunk);
					}
				}

				currentChunk = null;
			}

		} finally {
			reader.close();
		}
	}

	public void open(String filename) throws IOException {
		close();
		indexWriter = new FileWriter(filename);
	}

	private void parseMetaInfo(String line, Mp4Writer writer) {

		// MP4 track
		if (line.startsWith("$track.meta:")) {
			int pos = line.indexOf('=');
			Mp4QueryString meta = Mp4Utils.parseQueryString(line.substring(pos + 1));

			String type = meta.optString("type");
			int codecType = meta.optInt("codecType", 0);
			int timeScale = meta.optInt("timescale", 0);

			if ("video".equals(type)) {
				int width = meta.optInt("width", 0);
				int height = meta.optInt("height", 0);

				MMediaFormat mediaFormat = MMediaFormat.newVideoFormat(
						codecType, width, height);
				writer.setVideoFormat(mediaFormat);

			} else if ("audio".equals(type)) {
				MMediaFormat mediaFormat = MMediaFormat.newAudioFormat(
						codecType, timeScale);
				writer.setAudioFormat(mediaFormat);
			}

			// MP4 parameter sets data
		} else if (line.startsWith("$parameter.sets:")) {
			int pos = line.indexOf('=');
			Mp4QueryString meta = Mp4Utils.parseQueryString(line.substring(pos + 1));
			String sqs = meta.optString("sqs");
			String pps = meta.optString("pps");
			byte[] videoSqsSampleData = Mp4Utils.decodeHex(sqs);
			byte[] videoPpsSampleData = Mp4Utils.decodeHex(pps);

			writer.setParameterSet(videoSqsSampleData, videoPpsSampleData);

			// MP4 creation time
		} else if (line.startsWith("$meta:")) {
			int pos = line.indexOf('=');
			Mp4QueryString meta = Mp4Utils.parseQueryString(line.substring(pos + 1));

			long creationTime = meta.optLong("creation", 0);
			writer.setCreationTime(creationTime);

			// Unknown
		} else {
			System.err.println(line);
		}
	}

	public void writeChunkInfo(Mp4Chunk chunk) {
		String info = "$chunk,";
		info += chunk.getPayload();
		info += ",";
		info += chunk.getChunkId();
		info += ",";
		info += chunk.getSampleCount();
		info += ",";
		info += chunk.getChunkOffset();
		info += "\n";

		for (Mp4Sample sample : chunk.getSamples()) {
			if (sample.isSyncPoint()) {
				info += "@";
			} else {
				info += "#";
			}
			info += sample.getSampleId();
			info += ",";
			info += sample.getSampleSize();
			info += ",";
			info += sample.getSampleDuration();
			info += ",";
			info += chunk.getPayload();
			info += "\n";
		}
		writeIndexInfo(info);
	}

	private void writeIndexInfo(String info) {
		try {
			if (indexWriter == null) {
				return;
			}

			indexWriter.write(info);

		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	public void writeMetaInfo(Mp4Writer writer) {
		Mp4TrackInfo videoTrack = writer.getVideoTrack();
		Mp4TrackInfo audioTrack = writer.getAudioTrack();

		writeIndexInfo("$meta:creation=" + writer.getCreationTime() + "\n");

		if (videoTrack != null) {
			writeIndexInfo("$track.meta:" + videoTrack.getMetaString() + "\n");
		}

		if (audioTrack != null) {
			writeIndexInfo("$track.meta:" + audioTrack.getMetaString() + "\n");
		}
	}

	public void writeParameterSets(byte[] videoSqsSampleData,
			byte[] videoPpsSampleData) {
		String info = "$parameter.sets:";
		info += "sqs=";
		info += Mp4Utils.encodeHex(videoSqsSampleData);
		info += "&pps=";
		info += Mp4Utils.encodeHex(videoPpsSampleData);
		info += "\n";
		writeIndexInfo(info);
	}

}
