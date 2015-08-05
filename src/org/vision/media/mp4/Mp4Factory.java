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

import static org.vision.media.mp4.Mp4PropertyType.PT_BITS;
import static org.vision.media.mp4.Mp4PropertyType.PT_BYTES;
import static org.vision.media.mp4.Mp4PropertyType.PT_DATE;
import static org.vision.media.mp4.Mp4PropertyType.PT_FLOAT;
import static org.vision.media.mp4.Mp4PropertyType.PT_INT;
import static org.vision.media.mp4.Mp4PropertyType.PT_SIZE_TABLE;
import static org.vision.media.mp4.Mp4PropertyType.PT_STRING;

import java.util.Date;

/**
 * MP4 工厂类.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public final class Mp4Factory {

	private static final Mp4Factory instance = new Mp4Factory();

	/** 返回 MP4 日期对应的 java.util.Date 日期. */
	public static final Date getDate(long movieTime) {
		return new Date(movieTime * 1000 - 2082850791998L);
	}

	public static Mp4Factory getInstanae() {
		return instance;
	}

	/** 返回当前 MP4 时间戳. */
	public static long getMP4Timestamp() {
		Date date = new Date();
		return (date.getTime() / 1000L) + 2082844800L;
	}

	/**
	 * 添加版本和标记两个公共属性
	 * 
	 * @param atom
	 *            要添加属性的 Atom 对象.
	 */
	protected void addVersionAndFlags(Mp4Atom atom) {
		atom.addProperty(PT_INT, 1, "version");
		atom.addProperty(PT_INT, 3, "flags");
	}

	protected void init(Mp4Atom atom) {
		init(atom, 0);
	}

	/**
	 * 初始化指定的 Atom 对象.
	 * 
	 * @param atom
	 *            要初始化的 Atom 对象.
	 * @param version
	 *            要初始化的版本.
	 */
	protected void init(Mp4Atom atom, int version) {
		String type = Mp4Atom.getAtomType(atom.getType());
		if (Mp4Utils.isBlank(type)) {
			return;
		}

		final byte matrix[] = new byte[] { 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0x00, };

		char ch = type.charAt(0);
		if (ch == 'a') {
			if (type.equals("avc1")) {
				atom.addChildAtom("avcC");

				atom.setProperty("dataReferenceIndex", 1);
				atom.setProperty("hor", 72.0f);
				atom.setProperty("ver", 72.0f);
				atom.setProperty("frameCount", 1);
				atom.setProperty("compressorName", "AVC Coding");
				atom.setProperty("depth", 24);
				atom.setProperty("colorTable", 65535);

			} else if (type.equals("avcC")) {
				atom.setProperty("configurationVersion", 1);
				atom.setProperty("lengthSizeMinusOne", 0xFF);
				atom.setProperty("reserved2", 0x07);
			}

		} else if (ch == 'd') {
			if (type.equals("dinf")) {
				atom.addChildAtom("dref");

			} else if (type.equals("damr")) {
				atom.setProperty("vendor", 0x6d346970);
				atom.setProperty("decoderVersion", 1);
				atom.setProperty("framesPerSample", 1);

			} else if (type.equals("dref")) {
				atom.addChildAtom("url ");
				atom.setProperty("entryCount", 1);
			}

		} else if (ch == 'f') {
			if (type.equals("ftyp")) {
				atom.setProperty("major_brand", "mp42");
				atom.setProperty("compatible_brands", "mp42isom");
			}
			
		} else if (ch == 'h') {	
			if (type.equals("hdlr")) {
				atom.setProperty("name", "Handler");
			}

		} else if (ch == 'm') {
			if (type.equals("moov")) {
				atom.addChildAtom("mvhd");
				atom.addChildAtom("iods");

			} else if (type.equals("moof")) {
				atom.addChildAtom("mfhd");
				atom.addChildAtom("traf");

			} else if (type.equals("mdia")) {
				atom.addChildAtom("mdhd");
				atom.addChildAtom("hdlr");
				atom.addChildAtom("minf");

			} else if (type.equals("minf")) {
				atom.addChildAtom("dinf");
				atom.addChildAtom("stbl");

			} else if (type.equals("mp4a")) {
				atom.setProperty("dataReferenceIndex", 1);
				atom.setProperty("sampleSize", 16);

				atom.addChildAtom("esds");

			} else if (type.equals("mvex")) {
				atom.addChildAtom("trex");

			} else if (type.equals("mvhd")) {
				atom.setProperty("timescale", 1000);
				atom.setProperty("rate", 1.0f);
				atom.setProperty("volume", 1.0f);
				atom.setProperty("next_track_ID", 1);

				Mp4Property p = atom.findProperty("reserved2");
				if (p != null) {
					p.setValue(matrix);
				}
			}

		} else if (ch == 'r') {
			if (type.equals("root")) {
				atom.addChildAtom("ftyp");
				atom.addChildAtom("mdat");
				atom.addChildAtom("moov");
			}

		} else if (ch == 's') {
			if (type.equals("stbl")) {
				atom.addChildAtom("stsd");
				atom.addChildAtom("stts");
				atom.addChildAtom("stsz");

				atom.addChildAtom("stsc");
				atom.addChildAtom("stco");

			} else if (type.equals("samr")) {
				atom.setProperty("dataReferenceIndex", 1);
				atom.setProperty("channels", 2);
				atom.setProperty("sampleSize", 16);

				atom.addChildAtom("damr");
			}

		} else if (ch == 't') {
			if (type.equals("trak")) {
				atom.addChildAtom("tkhd");
				atom.addChildAtom("mdia");

			} else if (type.equals("trex")) {
				

			} else if (type.equals("traf")) {
				atom.addChildAtom("tfhd");
				atom.addChildAtom("trun");

			} else if (type.equals("tkhd")) {
				atom.setProperty("flags", 1);
				Mp4Property p = atom.findProperty("matrix");
				if (p != null) {
					p.setValue(matrix);
				}
			}

		} else if (ch == 'u') {
			if (type.equals("url ")) {
				atom.setProperty("flags", 1);
			}

		} else if (ch == 'v') {
			if (type.equals("vmhd")) {
				atom.setProperty("flags", 1);
			}
		}
	}

	protected void initProperties(Mp4Atom atom) {
		initProperties(atom, 0);
	}

	/**
	 * 初始化当前 Atom 的属性列表.
	 * 
	 * @param version
	 *            这个 Atom 的版本
	 */
	protected void initProperties(Mp4Atom atom, int version) {
		String type = Mp4Atom.getAtomType(atom.getType());
		if (Mp4Utils.isBlank(type)) {
			return;
		}

		char ch = type.charAt(0);
		if (ch == 'a') {
			// AVC atom
			if (type.equals("avc1")) {
				atom.addProperty(PT_BYTES, 6, "reserved1");
				atom.addProperty(PT_INT, 2, "dataReferenceIndex");
				atom.addProperty(PT_INT, 2, "version");
				atom.addProperty(PT_INT, 2, "level");
				atom.addProperty(PT_INT, 4, "vendor");
				atom.addProperty(PT_INT, 4, "temporalQuality");
				atom.addProperty(PT_INT, 4, "spatialQuality");
				atom.addProperty(PT_INT, 2, "width");
				atom.addProperty(PT_INT, 2, "height");
				atom.addProperty(PT_FLOAT, 4, "hor");
				atom.addProperty(PT_FLOAT, 4, "ver");
				atom.addProperty(PT_INT, 4, "dataSize");
				atom.addProperty(PT_INT, 2, "frameCount");
				atom.addProperty(PT_STRING, 32, "compressorName");
				atom.addProperty(PT_INT, 2, "depth");
				atom.addProperty(PT_INT, 2, "colorTable");
				atom.setExpectChild(true);

				// AVC atom
			} else if (type.equals("avcC")) {
				atom.addProperty(PT_INT, 1, "configurationVersion");
				atom.addProperty(PT_INT, 1, "AVCProfileIndication");
				atom.addProperty(PT_INT, 1, "profile_compatibility");
				atom.addProperty(PT_INT, 1, "AVCLevelIndication");
				atom.addProperty(PT_INT, 1, "lengthSizeMinusOne");

				atom.addProperty(PT_BITS, 3, "reserved2");
				atom.addProperty(PT_BITS, 5, "numOfSequenceParameterSets");
				atom.addProperty(PT_SIZE_TABLE, 0, "sequenceEntries");
				atom.addProperty(PT_INT, 1, "numOfPictureParameterSets");
				atom.addProperty(PT_SIZE_TABLE, 0, "pictureEntries");

			} else if (type.equals("alaw")) {
				atom.addProperty(PT_BYTES, 6, "reserved1");
				atom.addProperty(PT_INT, 2, "dataReferenceIndex");
				atom.addProperty(PT_INT, 2, "version");
				atom.addProperty(PT_INT, 2, "level");
				atom.addProperty(PT_INT, 4, "vendor");
				atom.addProperty(PT_INT, 2, "channels");
				atom.addProperty(PT_INT, 2, "sampleSize");
				atom.addProperty(PT_INT, 2, "packetSize");
				atom.addProperty(PT_INT, 4, "sampleRate");
				atom.addProperty(PT_INT, 2, "reserved2");
			}

		} else if (ch == 'b') {
			// Data information atom
			if (type.equals("btrt")) {
				atom.addProperty(PT_INT, 4, "bufferSizeDB");
				atom.addProperty(PT_INT, 4, "maxBitrate");
				atom.addProperty(PT_INT, 4, "avgBitrate");
			}

		} else if (ch == 'd') {

			// Data information atom
			if (type.equals("dinf")) {
				atom.setExpectChild(true);

				// AMR audio atom
			} else if (type.equals("damr")) {
				atom.addProperty(PT_INT, 4, "vendor");
				atom.addProperty(PT_INT, 1, "decoderVersion");
				atom.addProperty(PT_INT, 2, "modeSet");
				atom.addProperty(PT_INT, 1, "modeChangePeriod");
				atom.addProperty(PT_INT, 1, "framesPerSample");

				// Data reference atom
			} else if (type.equals("dref")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_INT, 4, "entryCount");
				atom.setExpectChild(true);
			}

		} else if (ch == 'e') {
			// MPEG-4 elementary stream descriptor atom
			if (type.equals("esds")) {
				addVersionAndFlags(atom);

				Mp4DescriptorProperty property = new Mp4DescriptorProperty(
						"descriptor", Mp4Descriptor.Mp4ESDescrTag);
				atom.addProperty(property);
			}

		} else if (ch == 'f') {
			// File type atom
			if (type.equals("ftyp")) {
				atom.addProperty(PT_STRING, 4, "major_brand");
				atom.addProperty(PT_INT, 4, "minor_version");
				atom.addProperty(PT_STRING, 0, "compatible_brands");

				// Free atom
			} else if (type.equals("free")) {
				// atom.addProperty(BytesProperty, 4, "size");
			}

		} else if (ch == 'g') {

			if (type.equals("g726")) {
				atom.addProperty(PT_BYTES, 6, "reserved1");
				atom.addProperty(PT_INT, 2, "dataReferenceIndex");
				atom.addProperty(PT_INT, 2, "version");
				atom.addProperty(PT_INT, 2, "level");
				atom.addProperty(PT_INT, 4, "vendor");
				atom.addProperty(PT_INT, 2, "channels");
				atom.addProperty(PT_INT, 2, "sampleSize");
				atom.addProperty(PT_INT, 2, "packetSize");
				atom.addProperty(PT_INT, 4, "sampleRate");
				atom.addProperty(PT_INT, 2, "reserved2");

			} else if (type.equals("gps ")) {
				atom.addProperty(PT_STRING, 0, "track");
			}

		} else if (ch == 'h') {
			// Handler reference atom
			if (type.equals("hdlr")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_STRING, 4, "type");
				atom.addProperty(PT_STRING, 4, "handler_type");
				atom.addProperty(PT_STRING, 4, "manufacturer");
				atom.addProperty(PT_BYTES, 4, "reserved1");
				atom.addProperty(PT_BYTES, 4, "reserved2");
				atom.addProperty(PT_STRING, 0, "name");
			}

		} else if (ch == 'i') {
			// Descriptor atom
			if (type.equals("iods")) {
				addVersionAndFlags(atom);

				Mp4DescriptorProperty property = new Mp4DescriptorProperty(
						"descriptor", Mp4Descriptor.Mp4FileIODescrTag);
				atom.addProperty(property);
			}

		} else if (ch == 'm') {
			// Movie atom
			if (type.equals("moov")) {
				atom.setExpectChild(true);

			} else if (type.equals("moof")) {
				atom.setExpectChild(true);

				// Media atom
			} else if (type.equals("mdia")) {
				atom.setExpectChild(true);
				
			} else if (type.equals("mvex")) {
				atom.setExpectChild(true);

				// Movie header atom
			} else if (type.equals("mvhd")) {
				addVersionAndFlags(atom);

				atom.addProperty(PT_DATE, 4, "creation_time");
				atom.addProperty(PT_DATE, 4, "modification_time");
				atom.addProperty(PT_INT, 4, "timescale");
				atom.addProperty(PT_INT, 4, "duration");
				atom.addProperty(PT_FLOAT, 4, "rate");
				atom.addProperty(PT_FLOAT, 2, "volume");
				atom.addProperty(PT_BYTES, 10, "reserved1");
				atom.addProperty(PT_BYTES, 60, "reserved2");
				atom.addProperty(PT_INT, 4, "next_track_ID");

				// Media header atom
			} else if (type.equals("mfhd")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_INT, 4, "sequence_number");

				// Media header atom
			} else if (type.equals("mdhd")) {
				addVersionAndFlags(atom);

				atom.addProperty(PT_DATE, 4, "creation_time");
				atom.addProperty(PT_DATE, 4, "modification_time");
				atom.addProperty(PT_INT, 4, "timescale");
				atom.addProperty(PT_INT, 4, "duration");
				atom.addProperty(PT_INT, 2, "language");
				atom.addProperty(PT_INT, 2, "quality");

				// Media information atom
			} else if (type.equals("minf")) {
				atom.setExpectChild(true);

				// MPEG-4 audio atom
			} else if (type.equals("mp4a")) {
				atom.addProperty(PT_BYTES, 6, "reserved1");
				atom.addProperty(PT_INT, 2, "dataReferenceIndex");
				atom.addProperty(PT_INT, 2, "version");
				atom.addProperty(PT_INT, 2, "level");
				atom.addProperty(PT_INT, 4, "vendor");
				atom.addProperty(PT_INT, 2, "channels");
				atom.addProperty(PT_INT, 2, "sampleSize");
				atom.addProperty(PT_INT, 2, "packetSize");
				atom.addProperty(PT_INT, 4, "sampleRate");
				atom.addProperty(PT_INT, 2, "reserved2");
				atom.setExpectChild(true);
			}

		} else if (ch == 'r') {
			// Root atom
			if (type.equals("root")) {
				atom.setExpectChild(true);
			}

		} else if (ch == 's') {
			// Sound media information header atom
			if (type.equals("smhd")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_BYTES, 4, "reserved");

				// AMR audio atom
			} else if (type.equals("samr")) {
				atom.addProperty(PT_BYTES, 6, "reserved1");
				atom.addProperty(PT_INT, 2, "dataReferenceIndex");
				atom.addProperty(PT_INT, 2, "version");
				atom.addProperty(PT_INT, 2, "level");
				atom.addProperty(PT_INT, 4, "vendor");
				atom.addProperty(PT_INT, 2, "channels");
				atom.addProperty(PT_INT, 2, "sampleSize");
				atom.addProperty(PT_BYTES, 4, "reserved2");
				atom.addProperty(PT_INT, 2, "timescale");
				atom.addProperty(PT_INT, 2, "reserved3");
				atom.setExpectChild(true);

				// Sample table atom
			} else if (type.equals("stbl")) {
				atom.setExpectChild(true);

				// Sample description atom
			} else if (type.equals("stsd")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_INT, 4, "entryCount");
				atom.setExpectChild(true);

				// Time-to-sample atom
			} else if (type.equals("stts")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_INT, 4, "entryCount");

				Mp4TableProperty table = new Mp4TableProperty("entries");
				table.addColumn("sample_count");
				table.addColumn("sampleDelta");
				atom.addProperty(table);

				// Sample-to-chunk atom
			} else if (type.equals("stsc")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_INT, 4, "entryCount");

				Mp4TableProperty table = new Mp4TableProperty("entries");
				table.addColumn("firstChunk");
				table.addColumn("samplesPerChunk");
				table.addColumn("sampleDescriptionIndex");
				atom.addProperty(table);

				// Sample size atom
			} else if (type.equals("stsz")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_INT, 4, "sampleSize");
				atom.addProperty(PT_INT, 4, "entryCount");

				Mp4TableProperty table = new Mp4TableProperty("entries");
				table.addColumn("sampleSize");
				atom.addProperty(table);

				// Sync sample atom
			} else if (type.equals("stss")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_INT, 4, "entryCount");

				Mp4TableProperty table = new Mp4TableProperty("entries");
				table.addColumn("sampleNumber");
				atom.addProperty(table);

				// Chunk offset atom
			} else if (type.equals("stco")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_INT, 4, "entryCount");

				Mp4TableProperty table = new Mp4TableProperty("entries");
				table.addColumn("chunkOffset");
				atom.addProperty(table);
			}

		} else if (ch == 't') {
			// Track atom
			if (type.equals("trak")) {
				atom.setExpectChild(true);

			} else if (type.equals("traf")) {
				atom.setExpectChild(true);

			} else if (type.equals("trun")) {
				addVersionAndFlags(atom);

				atom.addProperty(PT_INT, 4, "sample_count");
				atom.addProperty(PT_INT, 4, "data_offset");
				// atom.addProperty(PT_INT, 4, "sampleFlags");

			} else if (type.equals("tfhd")) {
				addVersionAndFlags(atom);

				atom.addProperty(PT_INT, 4, "track_ID");
				
				// 下面是可选的属性
				// atom.addProperty(PT_INT, 8, "base_data_offset");
				// atom.addProperty(PT_INT, 4, "index");
				// atom.addProperty(PT_INT, 4, "sample_duration");
				// atom.addProperty(PT_INT, 4, "sample_size");
				// atom.addProperty(PT_INT, 4, "sample_flags");

				// Track header atom
			} else if (type.equals("tkhd")) {
				addVersionAndFlags(atom);

				atom.addProperty(PT_DATE, 4, "creation_time");
				atom.addProperty(PT_DATE, 4, "modification_time");
				atom.addProperty(PT_INT, 4, "track_ID");
				atom.addProperty(PT_INT, 4, "reserved1");
				atom.addProperty(PT_INT, 4, "duration");
				atom.addProperty(PT_BYTES, 12, "reserved2");
				atom.addProperty(PT_FLOAT, 2, "volume");
				atom.addProperty(PT_INT, 2, "reserved3");
				atom.addProperty(PT_BYTES, 36, "matrix");
				atom.addProperty(PT_FLOAT, 4, "width");
				atom.addProperty(PT_FLOAT, 4, "height");
				
			} else if (type.equals("trex")) {
				addVersionAndFlags(atom);
				
				atom.addProperty(PT_INT, 4, "track_ID");
				atom.addProperty(PT_INT, 4, "default_sample_description_index");
				atom.addProperty(PT_INT, 4, "default_sample_duration");
				atom.addProperty(PT_INT, 4, "default_sample_size");
				atom.addProperty(PT_INT, 4, "default_sample_flags");

			} else if (type.equals("text")) {
				atom.addProperty(PT_INT, 4, "displayFlags");
				atom.addProperty(PT_INT, 4, "textJustification");
				atom.addProperty(PT_INT, 2, "backgroundColorR");
				atom.addProperty(PT_INT, 2, "backgroundColorG");
				atom.addProperty(PT_INT, 2, "backgroundColorB");
				atom.addProperty(PT_INT, 8, "defaultTextBox");
				atom.addProperty(PT_INT, 8, "reserved1");
				atom.addProperty(PT_INT, 2, "fontNumber");
				atom.addProperty(PT_INT, 2, "fontFace");
				atom.addProperty(PT_INT, 1, "reserved2");
				atom.addProperty(PT_INT, 2, "reserved3");
				atom.addProperty(PT_INT, 2, "foregroundColorR");
				atom.addProperty(PT_INT, 2, "foregroundColorG");
				atom.addProperty(PT_INT, 2, "foregroundColorB");
				atom.addProperty(PT_STRING, 0, "textName");
			}

		} else if (ch == 'u') {
			// Url atom
			if (type.equals("url ")) {
				addVersionAndFlags(atom);

			} else if (type.equals("udta")) {
				atom.addProperty(PT_BYTES, 0, "data");

			} else if (type.equals("uuid")) {
				atom.addProperty(PT_BYTES, 16, "uuid");

			} else if (type.equals("ulaw")) {
				atom.addProperty(PT_BYTES, 6, "reserved1");
				atom.addProperty(PT_INT, 2, "dataReferenceIndex");
				atom.addProperty(PT_INT, 2, "version");
				atom.addProperty(PT_INT, 2, "level");
				atom.addProperty(PT_INT, 4, "vendor");
				atom.addProperty(PT_INT, 2, "channels");
				atom.addProperty(PT_INT, 2, "sampleSize");
				atom.addProperty(PT_INT, 2, "packetSize");
				atom.addProperty(PT_INT, 4, "sampleRate");
				atom.addProperty(PT_INT, 2, "reserved2");
			}

		} else if (ch == 'v') {
			// Video media information header atom
			if (type.equals("vmhd")) {
				addVersionAndFlags(atom);
				atom.addProperty(PT_BYTES, 8, "reserved");
			}

		} else {
			// byte type_adpcm[] = {0x6D, 0x73, 0x00, 0x02, 0x00};
			// byte type_ima_adpcm[] = {0x6D, 0x73, 0x00, 0x02, 0x00};

			if (type.equals(".mp3")) {
				atom.addProperty(PT_BYTES, 6, "reserved1");
				atom.addProperty(PT_INT, 2, "dataReferenceIndex");
				atom.addProperty(PT_INT, 2, "version");
				atom.addProperty(PT_INT, 2, "level");
				atom.addProperty(PT_INT, 4, "vendor");
				atom.addProperty(PT_INT, 2, "channels");
				atom.addProperty(PT_INT, 2, "sampleSize");
				atom.addProperty(PT_INT, 2, "packetSize");
				atom.addProperty(PT_INT, 4, "sampleRate");
				atom.addProperty(PT_INT, 2, "reserved2");
			}
		}
	}

	/**
	 * 创建一个指定类型的 Atom 对象.
	 * 
	 * @param type
	 *            要创建的 Atom 的类型
	 * @return 返回创建的 Atom 对象.
	 */
	public Mp4Atom newAtom(String type) {
		Mp4Atom atom = new Mp4Atom(Mp4Atom.getAtomId(type));
		initProperties(atom);
		init(atom);
		return atom;
	}

	public Mp4Atom newAtomWithoutInit(String type) {
		Mp4Atom atom = new Mp4Atom(Mp4Atom.getAtomId(type));
		initProperties(atom);
		return atom;
	}

}
