package com.iflytek.roomprocessor.net.conn.impl;

import java.util.concurrent.Semaphore;

import org.apache.mina.core.session.IoSession;

import com.iflytek.roomprocessor.net.buffer.IBuffer;
import com.iflytek.roomprocessor.net.buffer.IOutBufferListener;
import com.iflytek.roomprocessor.net.conn.IConnection;

public abstract class BaseConnection implements IConnection , IOutBufferListener{
	
	public final static String RP_CONNECTION_KEY ="rp.conn";
	
	private final Semaphore writelock = new Semaphore(1, true);
	
	protected IoSession session = null;
	
	protected IBuffer inBuffer;
	
	protected IBuffer outBuffer;
	
	protected String remoteAddress;
	
	protected int remotePort;
	
	public IBuffer getInBuffer() {
		return inBuffer;
	}

	public void setInBuffer(IBuffer inBuffer) {
		this.inBuffer = inBuffer;
	}

	public IBuffer getOutBuffer() {
		return outBuffer;
	}

	public void setOutBuffer(IBuffer outBuffer) {
		this.outBuffer = outBuffer;
	}
	
	public Semaphore getLock(){
		return writelock;
	}

}
