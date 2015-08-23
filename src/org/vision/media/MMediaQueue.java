package org.vision.media;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MMediaQueue {
	public LinkedList<MMediaSample> mediaSamples = new LinkedList<MMediaSample>();

	/** 当前视频帧 . */
	private MMediaSample lastMediaSample;

	private Lock lock = new ReentrantLock();

	public void addMediaBuffer(MMediaBuffer mediaBuffer) {
		MMediaSample mediaSample = null;
		try {
			lock.lock();

			if (lastMediaSample == null) {
				lastMediaSample = new MMediaSample();
			}

			lastMediaSample.addBuffer(mediaBuffer);
			if (!mediaBuffer.isEnd()) {
				return;
			}

			mediaSample = lastMediaSample;
			lastMediaSample = null;
			
			if (mediaSample != null) {
				addMediaSample(mediaSample);
			}

		} finally {
			lock.unlock();
		}
	}

	public MMediaSample getMediaSample() {
		try {
			lock.lock();

			if (mediaSamples.isEmpty()) {
				return null;
			}
			return mediaSamples.getFirst();

		} finally {
			lock.unlock();
		}
	}

	public MMediaSample popMediaSample() {
		try {
			lock.lock();

			if (mediaSamples.isEmpty()) {
				return null;
			}
			return mediaSamples.removeFirst();

		} finally {
			lock.unlock();
		}
	}

	private void addMediaSample(MMediaSample mediaSample) {
		try {
			lock.lock();
			if (mediaSample.isSyncPoint()) {
				if (mediaSamples.size() > 10) {
					mediaSamples.clear();
				}
			}

			mediaSamples.addLast(mediaSample);

		} finally {
			lock.unlock();
		}
	}

	public void clear() {
		try {
			lock.lock();
			
			mediaSamples.clear();
			lastMediaSample = null;
			
		} finally {
			lock.unlock();
		}
	}
}
