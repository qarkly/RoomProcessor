package com.iflytek.roomprocessor.net.buffer;

import java.util.concurrent.LinkedBlockingQueue;
import com.iflytek.roomprocessor.api.IMessage;

public abstract  class BaseBuffer implements IBuffer{

	protected final LinkedBlockingQueue<IMessage> queue = new LinkedBlockingQueue<IMessage>();
	protected volatile long lastUpdateTime = 0;
	protected volatile boolean isrunning = false;
	protected volatile boolean paused = false;


	protected boolean isrunning(){
		return isrunning;
	}
	
	protected void run(){
			if(!isrunning){
				isrunning = true;
			}
	}
	
	protected void waitting(){
			if(isrunning)
				isrunning = false;
	}

	protected int size(){
		return queue.size();
	}

	@Override
	public boolean isEmpty() {
		return queue.isEmpty();
	}

	protected boolean isPuased(){
		return paused;
	}
	

	
}
