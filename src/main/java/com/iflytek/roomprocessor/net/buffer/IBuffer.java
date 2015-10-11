package com.iflytek.roomprocessor.net.buffer;

import com.iflytek.roomprocessor.api.IMessage;

public interface IBuffer {
	
	public void append(IMessage msg);
	
	public boolean isEmpty();
	
	public void pause();
	
	public void continues();
	
}
