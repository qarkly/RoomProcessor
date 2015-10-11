package com.iflytek.roomprocessor.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
	private final String roomId;
	private volatile int maxUserCount;
	private volatile String roomName;
	private volatile List<Integer> channelNos;
	private volatile int roomType;
	private volatile int bizType;
	private volatile String red5Id;
	private volatile long lastLeftTime;
	private volatile String roomBizNo;
	private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<String, Client>();
	
	public Room(String roomId, int maxUserCount, String roomName,
			List<Integer> channelNos, int roomType,int bizType) {
		super();
		this.roomId = roomId;
		this.maxUserCount = maxUserCount;
		this.roomName = roomName;
		this.channelNos = channelNos;
		this.roomType = roomType;
		this.bizType = bizType;
	}
	
	
	
	public String getRoomName(){
		return roomName;
	}
	
	
	public int getRoomType(){
		return roomType;
	}
	
	public String getRoomId() {
		return roomId;
	}
	public int getMaxUserCount() {
		return maxUserCount;
	}
	public long getLastLeftTime() {
		return lastLeftTime;
	}
	public void setLastLeftTime(long lastLeftTime) {
		this.lastLeftTime = lastLeftTime;
	}
	public String getRed5Id() {
		return red5Id;
	}	
	
	public int clientSize(){
		return clients.size();
	}
	
	public Client removeClient(String sId){
		return clients.remove(sId);
	}
	
	public Client addClient(Client client){
		return clients.putIfAbsent(client.getSid(), client);
	}
	
	public Client getClient(String sId){
		return clients.get(sId);
	}
	
	public List<Client> getClients(){
		Map<String, Client> map = new HashMap<String, Client>(clients);
		List<Client> list = new ArrayList<Client>(map.size());
		list.addAll(map.values());
		return list;
	}

	public void setRed5Id(String red5Id) {
		this.red5Id = red5Id;
	}

	public List<Integer> getChannelNos() {
		return channelNos;
	}

	public int getBizType() {
		return bizType;
	}
	
	public boolean isFull(){
		return maxUserCount <= clients.size();
	}



	public void setMaxUserCount(int maxUserCount) {
		this.maxUserCount = maxUserCount;
	}



	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}



	public void setChannelNos(List<Integer> channelNos) {
		this.channelNos = channelNos;
	}



	public void setRoomType(int roomType) {
		this.roomType = roomType;
	}



	public void setBizType(int bizType) {
		this.bizType = bizType;
	}



	public String getRoomBizNo() {
		return roomBizNo;
	}



	public void setRoomBizNo(String roomBizNo) {
		this.roomBizNo = roomBizNo;
	}
	
}
