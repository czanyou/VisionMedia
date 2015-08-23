package org.vision.media.hls;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vision.media.MMediaBuffer;
import org.vision.media.hls.PlayListReader.PlayItem;

public class HttpLiveClient {

	public static interface PlayFileHandler {
		public void onCompleted(int resultCode, PlayItem playItem);
	}

	public static interface PlayListHandler {
		public void onCompleted(int resultCode, PlayListReader playList);
	}

	public static interface PlayStreamHandler {
		public void onMediaSample(int flags, MMediaBuffer mediaBuffer);
	}

	protected static final Logger log = LoggerFactory
			.getLogger(HttpLiveClient.class);

	private String mBaseURL;

	private PlayItem mCurrentItem;

	private LinkedList<PlayItem> mDownloadList = new LinkedList<PlayItem>();

	private PlayFileHandler mFileHandler;

	private ByteBufferInputStream mInputStream;

	private boolean mIsRunning;

	private List<PlayItem> mPlayList = new ArrayList<PlayItem>();

	private PlayStreamHandler mStreamHandler;

	private MpegTSReader mStreamReader;

	private void asyncLoadPlayFile(final PlayItem playItem,
			final PlayFileHandler handler) {
		Executors.newSingleThreadExecutor().submit(new Runnable() {

			@Override
			public void run() {
				try {
					syncLoadPlayFile(playItem, handler);
				} catch (Exception e) {
					log.debug(e.getMessage(), e);
				}
			}

		});
	}

	private void asyncLoadPlayList(final String httpUrl,
			final PlayListHandler handler) {
		Executors.newSingleThreadExecutor().submit(new Runnable() {

			@Override
			public void run() {
				try {
					syncLoadPlayList(httpUrl, handler);
				} catch (Exception e) {
					log.debug(e.getMessage(), e);
				}
			}

		});
	}

	public void connect() {
		if (mBaseURL == null || mBaseURL.isEmpty()) {
			return;
		}

	}

	public void disconnect() {
		mIsRunning = false;
	}

	private boolean findPlayItem(int sequence) {
		for (PlayItem playItem : mPlayList) {
			if (sequence == playItem.sequence) {
				return true;
			}
		}

		return false;
	}

	private String getParentPath(String path) {
		if (path == null || path.length() <= 0) {
			return "";
			
		} else if (path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		}
		
		int pos = path.lastIndexOf('/');
		if (pos <= 0) {
			return "";
		}
		
		return path.substring(0, pos);
	}
	
	public PlayStreamHandler getStreamHandler() {
		return mStreamHandler;
	}
	
	private boolean isValidURL(String url) {
		try {
			new URL(url);
			return true;
			
		} catch (MalformedURLException e) {
			return false;
		}
	}

	private void onLoadPlayItem(PlayItem playItem) {
		if (findPlayItem(playItem.sequence)) {
			return;
		}

		mPlayList.add(playItem);
		mDownloadList.offer(playItem);
		
		if (isValidURL(playItem.url)) {
			return;
		}

		try {
			URL url = new URL(mBaseURL);
			String basePath = getParentPath(url.getPath());

			StringBuilder sb = new StringBuilder();
			sb.append(url.getProtocol()).append("://");
			sb.append(url.getHost());
			if (url.getPort() > 0) {
				sb.append(":");
				sb.append(url.getPort());
			}
			sb.append(basePath);
			sb.append("/");
			sb.append(playItem.url);

			playItem.url = sb.toString();
		} catch (Exception e) {

		}

		log.warn("play item:" + playItem.url + "(" + playItem.sequence + ")");

		onStartBuffering();
	}

	private void onLoadPlayList(List<PlayItem> playList) {
		if (playList == null) {
			return;
		}

		for (PlayItem playItem : playList) {
			onLoadPlayItem(playItem);
		}
	}

	private void onReadPlayFile() {
		if (mStreamReader == null) {
			mStreamReader = new MpegTSReader("test");
		}

		mStreamReader.setInputStream(mInputStream);

		while (true) {
			if (!mStreamReader.advance()) {
				break;
			}

			MMediaBuffer mediaBuffer = mStreamReader.currentSample();

			if (mStreamHandler != null) {
				mStreamHandler.onMediaSample(0, mediaBuffer);
			}
		}
	}

	private void onStartBuffering() {
		if (mCurrentItem == null) {
			mCurrentItem = mDownloadList.poll();
			if (mCurrentItem == null) {
				return;
			}
		}

		if (mFileHandler != null) {
			return;
		}

		mFileHandler = new PlayFileHandler() {

			@Override
			public void onCompleted(int resultCode, PlayItem playItem) {
				mFileHandler = null;

				if (resultCode < 0) {
					onStartBuffering();
					return;
				}

				mCurrentItem = null;
				onStartBuffering();
			}

		};

		asyncLoadPlayFile(mCurrentItem, mFileHandler);
	}

	public void runLoop() {
		mIsRunning = true;
		while (mIsRunning) {
			asyncLoadPlayList(mBaseURL, new PlayListHandler() {

				@Override
				public void onCompleted(int resultCode, PlayListReader playList) {
					if (resultCode < 0) {
						return;
					}

					onLoadPlayList(playList.getPlayList());
				}

			});

			sleep(3 * 1000);
		}
	}

	public void setStreamHandler(PlayStreamHandler streamHandler) {
		mStreamHandler = streamHandler;
	}

	public void setURL(String url) {
		mBaseURL = url;
	}

	private void sleep(int timeout) {
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {

		}
	}

	private void syncLoadPlayFile(PlayItem playItem, PlayFileHandler handler) {
		URL url = null;

		if (mInputStream == null) {
			mInputStream = new ByteBufferInputStream(1024 * 1024 * 4);
		}

		try {
			String httpUrl = playItem.url;

			url = new URL(httpUrl);
			URLConnection conn = url.openConnection();
			InputStream inStream = conn.getInputStream();

			int totalSize = 0;

			byte[] readBuffer = new byte[1024 * 64];
			while (true) {
				int ret = inStream.read(readBuffer);
				if (ret < 0) {
					break;
				}

				mInputStream.put(readBuffer, 0, ret);

				totalSize += ret;
			}

			inStream.close();

			log.info("loadStreamFile totalSize: " + totalSize);
			onReadPlayFile();

			if (handler != null) {
				handler.onCompleted(0, playItem);
			}

			return;

		} catch (MalformedURLException e) {
			log.warn(e.getMessage());

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		if (handler != null) {
			handler.onCompleted(-1, playItem);
		}
	}

	private void syncLoadPlayList(String httpUrl, PlayListHandler handler) {
		URL url = null;
		HttpURLConnection connection = null;

		try {
			url = new URL(httpUrl);
			connection = (HttpURLConnection) url.openConnection();
			InputStream inStream = connection.getInputStream();
			InputStreamReader reader = new InputStreamReader(inStream);

			PlayListReader listReader = new PlayListReader();
			listReader.parsePlayList(reader);

			if (handler != null) {
				handler.onCompleted(0, listReader);
			}

			return;

			// onLoadPlayList(listReader.getPlayList());

		} catch (Exception e) {

			try {
				String error = connection.getResponseMessage();
				log.error(error);

			} catch (IOException ex) {
			}

		}

		if (handler != null) {
			handler.onCompleted(-1, null);
		}
	}

}
