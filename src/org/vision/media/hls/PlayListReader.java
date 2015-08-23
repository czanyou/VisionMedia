package org.vision.media.hls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP Live Streaming index file reader.
 * 
 * @author m
 *
 */
public class PlayListReader {

	/** Play List Item. */
	public static class PlayItem {
		public int duration;
		public String url;
		public int sequence;
	}

	protected static final Logger log = LoggerFactory
			.getLogger(PlayListReader.class);
	private PlayItem lastPlayItem;

	private int mediaSequence = 0;

	private List<PlayItem> playList = new ArrayList<PlayItem>();

	private int targetDuraction = 0;

	public PlayListReader() {

	}

	public int getMediaSequence() {
		return mediaSequence;
	}

	public List<PlayItem> getPlayList() {
		return playList;
	}

	public int getTargetDuraction() {
		return targetDuraction;
	}

	private int parseInt(String value) {
		try {
			return Integer.valueOf(value);

		} catch (Exception e) {
		}

		return 0;
	}

	private void parseLine(String token) {
		if (token == null) {
			return;

		} else if (token.isEmpty()) {
			return;

		}

		if (token.startsWith("#EXT")) {
			String name = token;
			String value = null;
			int pos = token.indexOf(':');
			if (pos > 0) {
				name = token.substring(0, pos).trim();
				value = token.substring(pos + 1).trim();
			}

			if (value == null) {
				return;
			}

			if (name.equals("#EXT-X-TARGETDURATION")) {
				targetDuraction = parseInt(value);

			} else if (name.equals("#EXT-X-MEDIA-SEQUENCE")) {
				mediaSequence = parseInt(value);

			} else if (name.equals("#EXTINF")) {
				pos = value.indexOf(',');
				if (pos > 0) {
					value = value.substring(0, pos);
				}

				lastPlayItem = new PlayItem();
				lastPlayItem.duration = parseInt(value);
			}

		} else {
			if (lastPlayItem != null) {
				lastPlayItem.url = token;
				lastPlayItem.sequence = mediaSequence + playList.size();

				playList.add(lastPlayItem);
				lastPlayItem = null;
			}
		}
	}

	public void parsePlayList(Reader reader) {
		BufferedReader bufferedReader = null;

		playList.clear();

		try {
			bufferedReader = new BufferedReader(reader);
			try {
				while (true) {
					String token = bufferedReader.readLine();
					if (token == null) {
						break;
					}

					// log.debug(token);
					parseLine(token);
				}

			} catch (IOException e) {
			}

		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public void parsePlayList(String content) {
		if (content == null || content.isEmpty()) {
			return;
		}

		StringTokenizer st = new StringTokenizer(content, "\r\n");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			log.debug(token);

			parseLine(token);
		}
	}

	public void setMediaSequence(int mediaSequence) {
		this.mediaSequence = mediaSequence;
	}

	public void setTargetDuraction(int targetDuraction) {
		this.targetDuraction = targetDuraction;
	}
}
