package com.iflytek.roomprocessor.jmx.mxbeans;

import javax.management.MXBean;

@MXBean
public interface ConfigModifyMXBean {

	public long getBufferDelayTime();
	
	public void setBufferDelayTime(long time);
	
	public int getMaxSize();
	
	public void setMaxSize(int size);
	
	public long getRoomTimeout();
	
	public void setRoomTimeout(long timeout);
	
	public void setRefreshTime(long refreshtime);
	
	public long getRefreshTime();
	
}
