package com.iflytek.roomprocessor.global;


import java.util.HashSet;
import java.util.List;

import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.conn.IRPConnManager;
import com.iflytek.roomprocessor.net.conn.impl.RPConnManager;
import com.iflytek.roomprocessor.net.message.Event;
import com.iflytek.roomprocessor.net.message.StreamSend;
import com.iflytek.roomprocessor.net.message.MessageType.SendType;

public class TargetSend {
	
	public static void sendToClient(String sId,String nm,Object msg){
		StreamSend send = new StreamSend(SendType.TO_CLIENT);
		send.setTar(sId);
		Event event = new Event();
		event.setNm(nm);
		event.setMsg(msg);
		send.setD(event);
		
		IRPConnManager manager = RPConnManager.getConnManager();
		Client client = RP.findSIdInUserInfo(sId).getClient(sId);
		IConnection connection = manager.getConnection(client.getClientId());
		connection.appendOut(send);
	}
	
	public static void sendToUser(String hashId,String nm,Object msg){
		StreamSend send = new StreamSend(SendType.TO_USER);
		send.setTar(hashId);
		Event event = new Event();
		event.setNm(nm);
		event.setMsg(msg);
		send.setD(event);
		
		UserInfo userInfo = RP.findUserByHashId(hashId);
		List<Client> list = userInfo.getClients();
		IRPConnManager manager = RPConnManager.getConnManager();
		HashSet<IConnection> set = new HashSet<IConnection>();
		for (Client client : list) {
			IConnection connection = manager.getConnection(client.getClientId());
			if(set.add(connection)){
				connection.appendOut(send);
			}
		}
	}
	
	public static void sendToRoom(String roomId,String nm,Object msg){
		StreamSend send = new StreamSend(SendType.TO_ROOM);
		send.setTar(roomId);
		Event event = new Event();
		event.setNm(nm);
		event.setMsg(msg);
		send.setD(event);
		
		Room room = RP.findRoomByRoomId(roomId);
		List<Client> list = room.getClients();
		IRPConnManager manager = RPConnManager.getConnManager();
		HashSet<IConnection> set = new HashSet<IConnection>();
		for (Client client : list) {
			IConnection connection = manager.getConnection(client.getClientId());
			if(set.add(connection)){
				connection.appendOut(send);
			}
		}
	}
	
	public static void sendToAll(String nm,Object msg){
		StreamSend send = new StreamSend(SendType.TO_ALL);
		send.setTar("");
		Event event = new Event(nm,msg);
		send.setD(event);
		
		IRPConnManager manager = RPConnManager.getConnManager();
		List<IConnection> list = manager.getConnections();
		for (IConnection connection : list) {
			connection.appendOut(send);
		}
	}
	
	public static void sendToRoomExceptOne(String sId,String roomId,String nm,Object msg){
		StreamSend send = new StreamSend(SendType.TO_ROOM_EXCEPT_ONE);
		send.setTar(String.format("{%s}|{%s}", sId,roomId));
		Event event = new Event(nm, msg);
		send.setD(event);
		
		StreamSend toRoom = new StreamSend(SendType.TO_ROOM);
		toRoom.setTar(roomId);
		toRoom.setD(event);
		
		
		IRPConnManager manager = RPConnManager.getConnManager();
		Client exceptclient = RP.findSIdInUserInfo(sId).getClient(sId);
		IConnection exceptconnection = manager.getConnection(exceptclient.getClientId());
		exceptconnection.appendOut(send);
		
		Room room = RP.findRoomByRoomId(roomId);
		List<Client> list = room.getClients();
		HashSet<IConnection> set = new HashSet<IConnection>();
		for (Client client : list) {
			IConnection connection = manager.getConnection(client.getClientId());
			if(connection != exceptconnection && set.add(connection)){
				connection.appendOut(toRoom);
			}
		}
	}

}
