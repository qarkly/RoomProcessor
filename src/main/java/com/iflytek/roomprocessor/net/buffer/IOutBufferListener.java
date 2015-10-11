package com.iflytek.roomprocessor.net.buffer;

import java.util.List;

import com.iflytek.roomprocessor.api.IMessage;

public interface IOutBufferListener {
	
	public void notify(List<IMessage> list);

}
