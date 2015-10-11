package com.iflytek.roomprocessor.common.config;

import java.util.Properties;

import com.iflytek.roomprocessor.jmx.mxbeans.ConfigModifyMXBean;

public class ConfigModify implements ConfigModifyMXBean {

	private Properties properties = null;
	public ConfigModify(){
		Config config = Config.getInstance();
		properties = config.getProperties();
	}
	@Override
	public long getBufferDelayTime() {
		String delaytime = properties.getProperty("delaytime");
		return Long.valueOf(delaytime);
	}

	@Override
	public void setBufferDelayTime(long time) {
		String delaytime = String.valueOf(time);
		properties.setProperty("delaytime", delaytime);
	}

	@Override
	public int getMaxSize() {
		String maxsize = properties.getProperty("maxsize");
		return Integer.valueOf(maxsize);
	}

	@Override
	public void setMaxSize(int size) {
		String maxsize = String.valueOf(size);
		properties.setProperty("maxsize", maxsize);

	}

	@Override
	public long getRoomTimeout() {
		String roomtimeout = properties.getProperty("roomtimeout");
		return Long.valueOf(roomtimeout);
	}

	@Override
	public void setRoomTimeout(long timeout) {
		String roomtimeout = String.valueOf(timeout);
		properties.setProperty("roomtimeout", roomtimeout);
	}
	@Override
	public void setRefreshTime(long refreshtime) {
		String refresh = String.valueOf(refreshtime);
		properties.setProperty("refreshtimer", refresh);
		
	}
	@Override
	public long getRefreshTime() {
		String time = properties.getProperty("refreshtimer");
		return Long.valueOf(time);
	}

}
