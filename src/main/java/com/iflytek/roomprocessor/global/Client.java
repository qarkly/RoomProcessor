package com.iflytek.roomprocessor.global;

public class Client {

	private final String sid;
	
	private final String ip;
	
	private final String clientId;
	
	private String hashid;
	
	private String roomid;
	
	
	public Client(String sid, String ip, String clientId) {
		super();
		this.sid = sid;
		this.ip = ip;
		this.clientId = clientId;
	}

	public String getSid(){
		return sid;
	}
	
	public String getIp(){
		return ip;
	}
	
	public String getClientId(){
		return clientId;
	}

	public String getHashid() {
		return hashid;
	}

	public void setHashid(String hashid) {
		this.hashid = hashid;
	}

	public String getRoomid() {
		return roomid;
	}

	public void setRoomid(String roomid) {
		this.roomid = roomid;
	}
}
