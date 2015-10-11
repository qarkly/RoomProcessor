package com.iflytek.roomprocessor.net.conn;

import java.util.List;

import org.apache.mina.core.session.IoSession;

import com.iflytek.roomprocessor.api.IMessage;

public interface IConnection {
	
	public void appendIn(IMessage msg);
	
	public void appendOut(IMessage msg);
	
	public void flushOut();
	
	public void setIoSession(IoSession protocolSession);
	
	public void write(List<IMessage> msgs);
	
	public String getClinetId();
	
	public boolean isConnected();
	
	public void close();
	
	public String getRemoteAddress();
	
	public int getRemotePort();
	
	public boolean isInBufferEmpty();
	
	public boolean isOutBufferEmpty();
	
	public void pause();
	
	public void continues();
	
	public void removeIoSession();
	

}
