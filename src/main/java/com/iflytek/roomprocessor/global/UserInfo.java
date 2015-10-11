package com.iflytek.roomprocessor.global;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;



public class UserInfo {

	private String hashId;
	private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<String, Client>();
	public String getHashId() {
		return hashId;
	}
	public void setHashId(String hashId) {
		this.hashId = hashId;
	}

	public Client putClient(Client client){
		return clients.putIfAbsent(client.getSid(), client);
	}
	
	public Client getClient(String sId){
		return clients.get(sId);
	}
	
	public void removeClient(String sId){
		clients.remove(sId);
	}
	
	public List<Client> getClients(){
		if(isEmpty())
			return null;
		List<Client> list = new ArrayList<Client>();
		for(String key:clients.keySet()){
			list.add(clients.get(key));
		}
		return list;
	}
	
	public boolean isEmpty(){
		return clients.size() == 0;
	}
	
	
}
