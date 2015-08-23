package org.vision.media;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 代表一个完整的媒体帧, 比如一帧完整的图像, 或一个完整的 P 帧等等.
 * 
 * @author ChengZhen(anyou@msn.com)
 */
public class MMediaSample {

	/** 这一帧包含的数据块. */
	private List<MMediaBuffer> mediaBuffers;
	
	public MMediaSample() {
		
	}
	
	/** 添加一个数据块. */
	public void addBuffer(MMediaBuffer mediaBuffer) {
		if (mediaBuffers == null) {
			mediaBuffers = new ArrayList<MMediaBuffer>();
		}
		
		mediaBuffers.add(mediaBuffer);
	}
	
	/** 指出这一帧是否是一个同步点. */
	public boolean isSyncPoint() {
		MMediaBuffer mediaBuffer = getBuffer(0);
		if (mediaBuffer == null) {
			return false;
		}
		
		return mediaBuffer.isSyncPoint();
	}
	
	/** 返回指定的索引的数据块. */
	public MMediaBuffer getBuffer(int index) {
		if (mediaBuffers == null) {
			return null;
			
		} else if (index < 0 || index >= mediaBuffers.size()) {
			return null;
		}
		
		return mediaBuffers.get(index);
	}
	
	/** 返回这一帧包含的数据块的数目. */
	public int getBufferCount() {
		if (mediaBuffers == null) {
			return 0;
		}
		
		return mediaBuffers.size();
	}
	
	/** 返回包含这一帧的所有数据的缓存区. */
	public ByteBuffer getData() {
		if (mediaBuffers == null) {
			return null;
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(getSize());
		for (MMediaBuffer mediaBuffer : mediaBuffers) {
			ByteBuffer data = mediaBuffer.getData();
			data.mark();
			buffer.put(data);
			data.reset();
		}
		
		buffer.flip();
		return buffer;
	}

	/** 返回这一帧总共的数据的长度. */
	public int getSize() {
		if (mediaBuffers == null) {
			return 0;
		}
		
		int size = 0;
		for (MMediaBuffer mediaBuffer : mediaBuffers) {
			size += mediaBuffer.getSize();
		}
		
		return size;
	}
	
}
