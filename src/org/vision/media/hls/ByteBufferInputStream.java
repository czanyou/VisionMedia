package org.vision.media.hls;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * @author m
 *
 */
public class ByteBufferInputStream extends InputStream {

	private byte[] mBuffer;

	private int mHead;

	private int mTail;

	public ByteBufferInputStream(int length) {
		mBuffer = new byte[length];
		mHead = 0;
		mTail = 0;
	}

	/**
	 * Returns the number of remaining bytes that can be read (or skipped over)
	 * from this input stream.
	 * <p>
	 * The value returned is <code>count&nbsp;- pos</code>, which is the number
	 * of bytes remaining to be read from the input buffer.
	 *
	 * @return the number of remaining bytes that can be read (or skipped over)
	 *         from this input stream without blocking.
	 */
	@Override
	public synchronized int available() throws IOException {
		if (mHead <= mTail) {
			return mTail - mHead;

		} else {

			return mTail + (mBuffer.length - mHead);
		}
	}

	public synchronized int freeSize() {
		if (mHead == mTail) {
			return mBuffer.length - 1;

		} else if (mHead < mTail) {
			return mBuffer.length - (mTail - mHead) - 1;

		} else {
			return mHead - mTail - 1;
		}
	}

	/**
	 * Closing a <tt>ByteBufferInputStream</tt> has no effect. The methods in
	 * this class can be called after the stream has been closed without
	 * generating an <tt>IOException</tt>.
	 * <p>
	 */
	public void close() throws IOException {
	}

	/**
	 * Tests if this <code>InputStream</code> supports mark/reset. The
	 * <code>markSupported</code> method of <code>ByteArrayInputStream</code>
	 * always returns <code>true</code>.
	 *
	 */
	public boolean markSupported() {
		return false;
	}

	@Override
	public synchronized int read() throws IOException {
		if (mHead == mTail) {
			return -1;
		}

		int ret = mBuffer[mHead++] & 0xff;
		if (mHead >= mBuffer.length) {
			mHead = 0;
		}

		return ret;
	}

	public synchronized int put(byte buffer[], int offset, int length) {
		if (length > freeSize()) {
			return -1;
		}

		if (mHead > mTail) {
			System.arraycopy(buffer, offset, mBuffer, mTail, length);
			mTail += length;
			return length;
		}

		int count = mBuffer.length - mTail;
		if (count >= length) {
			System.arraycopy(buffer, offset, mBuffer, mTail, length);
			mTail += length;
			if (mTail >= mBuffer.length) {
				mTail = 0;
			}

			return length;
			
		} else {
			int pos = offset;
			int leftover = length;

			System.arraycopy(buffer, pos, mBuffer, mTail, count);
			mTail = 0;
			pos += count;
			leftover -= count;

			System.arraycopy(buffer, pos, mBuffer, mTail, leftover);
			mTail += leftover;

			return length;
		}
	}

	public synchronized int read(byte buffer[], int offset, int length) {
		if (buffer == null) {
			throw new NullPointerException();

		} else if (offset < 0 || length < 0 || length > buffer.length - offset) {
			throw new IndexOutOfBoundsException();
		}

		if (mHead == mTail) {
			return -1;

		} else if (mHead < mTail) {
			int count = mTail - mHead;
			if (length > count) {
				length = count;
			}

			System.arraycopy(mBuffer, mHead, buffer, offset, length);
			mHead += length;
			return length;

		} else {
			int count = mBuffer.length - mHead;
			if (length <= count) {
				System.arraycopy(mBuffer, mHead, buffer, offset, length);
				if (length == count) {
					mHead = 0;

				} else {
					mHead += length;
				}

				return length;

			} else {
				int pos = offset;
				int leftover = length;

				System.arraycopy(mBuffer, mHead, buffer, pos, count);
				mHead = 0;
				pos += count;
				leftover -= count;

				//
				count = mTail;
				if (leftover > count) {
					leftover = count;
				}

				System.arraycopy(mBuffer, mHead, buffer, pos, leftover);
				mHead += leftover;
				return length;
			}
		}
	}

}
