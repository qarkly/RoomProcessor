package com.iflytek.roomprocessor.net.conn.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.conn.IRPConnManager;

public class RPConnManager implements IRPConnManager {
	
	
	private  ConcurrentHashMap<String, IConnection> connections ;
	
	private static class Holder{
		private static IRPConnManager manager = new RPConnManager();
	}
	
	public static IRPConnManager getConnManager(){
		return Holder.manager;
	}
	
	private RPConnManager(){
		connections =  new ConcurrentHashMap<String, IConnection>();
	}
	
	

	public  IConnection getConnection(String clienId) {
		return connections.get(clienId);
	}

	public IConnection createConnection(String clientId) {
		IConnection connection = new RPConnection(clientId);
		connections.putIfAbsent(clientId, connection);
		return connection;
	}

	public IConnection removeConnection(String clientId) {
		return connections.remove(clientId);
		
	}

	public Collection<IConnection> removeConnections() {
		ArrayList<IConnection> list = new ArrayList<IConnection>(connections.size());
		list.addAll(connections.values());
		return list;
	}

	@Override
	public List<IConnection> getConnections() {
		Map<String, IConnection> map = new HashMap<String, IConnection>(connections);
		List<IConnection> list = new ArrayList<IConnection>();
		list.addAll(map.values());
		return list;
	}

}
