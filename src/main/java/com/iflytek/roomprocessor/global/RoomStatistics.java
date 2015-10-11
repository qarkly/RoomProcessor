package com.iflytek.roomprocessor.global;

import com.iflytek.roomprocessor.common.config.Config;

public class RoomStatistics {
	public int getAllRoomMaxUserCount() {
		return allRoomMaxUserCount;
	}
	public void setAllRoomMaxUserCount(int allRoomMaxUserCount) {
		this.allRoomMaxUserCount = allRoomMaxUserCount;
	}
	public int getRoomCount() {
		return roomCount;
	}
	public void setRoomCount(int roomCount) {
		this.roomCount = roomCount;
	}
	public int getUserCount() {
		return userCount;
	}
	public void setUserCount(int userCount) {
		this.userCount = userCount;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	private int allRoomMaxUserCount = 0;
	private int roomCount = 0;
	private int userCount = 0;
	private String ip ="127.0.0.1";
	private int port = 0;
	public RoomStatistics(){
		Config config = Config.getInstance();
		this.ip = config.getLocalhost();
		this.port = config.getPort();
	}
	
	@Override
	public String toString() {
		return String.format("{allRoomMaxUserCount:%d,roomCount:%d,userCount:%d,ip:%s,prot:%d}", allRoomMaxUserCount,roomCount,userCount,ip,port);
	}
	
	
}
