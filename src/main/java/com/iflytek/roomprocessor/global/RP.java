package com.iflytek.roomprocessor.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.exception.SysException;
import com.iflytek.roomprocessor.components.rabbitmq.Subscriber;

public class RP {
	public static final List<Subscriber> Subscribers = new ArrayList<Subscriber>();
	private static final CopyOnWriteArrayList<UserInfo> users = new CopyOnWriteArrayList<UserInfo>();
	private static final LinkedList<Room> rooms = new LinkedList<Room>();
//	private static final ReentrantLock userlock = new ReentrantLock();
	
	
	/**
	 * 根据用户hashid查找用户信息，该用户下的所有sid及对应的clientid
	 * @param hashId
	 * @return
	 */
	public static UserInfo findUserByHashId(String hashId){
		Iterator<UserInfo> iterator = users.iterator();
		while (iterator.hasNext()) {
			UserInfo userInfo = iterator.next();
			if(userInfo.getHashId().equals(hashId))
				return userInfo;
		}
		return null;
	}
	/**
	 * 添加用户信息
	 * @param userInfo
	 * @return
	 */
	public static boolean addUser(UserInfo userInfo){
			Iterator<UserInfo> iterator = users.iterator();
			while (iterator.hasNext()) {
				UserInfo userInfo2 = (UserInfo) iterator.next();
				if(userInfo2.getHashId().equals(userInfo.getHashId()))
					return false;
			}
			return users.add(userInfo);
		
	}
	
	public static UserInfo findSIdInUserInfo(String sId){
		Iterator<UserInfo> iterator = users.iterator();
		while (iterator.hasNext()) {
			UserInfo userInfo = iterator.next();
			Client client = userInfo.getClient(sId);
			if(client !=null)
				return userInfo;
		}
		return  null;
	}
	
	public static boolean removeUser(UserInfo userInfo){
		return users.remove(userInfo);
	}
	
	public static List<Client> getAllClients(){
		List<Client> list = new ArrayList<Client>();
		for (UserInfo userInfo : users) {
			list.addAll(userInfo.getClients());
		}
		return list;
	}
	
	/**
	 * 根据roomid查找房间信息
	 * @param roomId
	 * @return
	 */
	public static synchronized Room findRoomByRoomId(String roomId){
		Iterator<Room> iterator = rooms.iterator();
		while (iterator.hasNext()) {
			Room room = iterator.next();
			if(room.getRoomId().equals(roomId))
				return room;
		}
		return null;
	}
	
	public static synchronized Room removeSIdFromRoom(String sId){
		Iterator<Room> iterator = rooms.iterator();
		while (iterator.hasNext()) {
			Room room = iterator.next();
			if(room.removeClient(sId) != null)
				return room;
		}
		return null;
	}
	
	/**
	 * 添加房间信息,如果房间已存在则不予添加,返回false
	 * @param room
	 * @return
	 */
	public static synchronized boolean addRoom(Room room){
			for (Room nextroom : rooms) {
				if(nextroom.getRoomId().equals(room.getRoomId()))
					return false;
			}
			rooms.add(room);
			return true;
	}
	
	/**
	 * 根据roomid移除房间,不存在则抛出房间不存在异常
	 * @param roomId
	 * @return
	 */
	public static synchronized boolean removeRoom(String roomId){
			Room room = null;
			for (Room nextRoom : rooms) {
				if(nextRoom.getRoomId().equals(roomId)){
					room = nextRoom;
					break;
				}
			}
			if(room ==null)
				throw new SysException(String.format("房间号：%s 不存在", roomId));
			return rooms.remove(room);
	}
	
	/**
	 * 统计房间进程下所有房间信息
	 * @return
	 */
	public static synchronized RoomStatistics getAllRoomMessage(){
			RoomStatistics roomstt = new RoomStatistics();
			roomstt.setRoomCount(rooms.size());
			int allUserCount = 0;
			int allRoomMaxUserCount = 0;
			for(Room nextRoom:rooms){
				allRoomMaxUserCount += nextRoom.getMaxUserCount();
				allUserCount += nextRoom.clientSize();
			}
			roomstt.setAllRoomMaxUserCount(allRoomMaxUserCount);
			roomstt.setUserCount(allUserCount);
			return roomstt;
	}
	/**
	 * 移除人数为空且最后一个离开房间超过规定时间的房间
	 * @return 移除的房间id
	 */
	public static synchronized String[] removeEmptyRoom(){
		Config config = Config.getInstance();
		List<String> emptyroomidlist = null;
		ListIterator<Room> iterator = rooms.listIterator();
		while (iterator.hasNext()) {
			Room room = iterator.next();
			long currentTime = System.currentTimeMillis();
			if(room.clientSize() == 0 && currentTime - room.getLastLeftTime() > config.getRoomTimeOut()*60*1000){
				if(emptyroomidlist == null)
					emptyroomidlist = new ArrayList<String>();
				emptyroomidlist.add(room.getRoomId());
				iterator.remove();
			}
		}
		if(emptyroomidlist != null){
			String[] roomlist = new String[emptyroomidlist.size()];
			return emptyroomidlist.toArray(roomlist);
		}
		return null;
	}
	/**
	 * 获取房间的lastlefttime和usercount 便于更新room节点下的roomprocessor节点信息
	 * @return
	 */
	public static synchronized List<Map<String, Object>> getRoomMessage(){
		Config config = Config.getInstance();
		String roomprocessorId = config.getLocalhost()+":"+config.getPort();
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		ListIterator<Room> iterator = rooms.listIterator();
		while (iterator.hasNext()) {
			Room room = iterator.next();
			Map<String,Object> rMap = new HashMap<String, Object>();
			rMap.put("roomId", room.getRoomId());
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("lastLogoutTime", room.getLastLeftTime());
			map.put("userCount", room.clientSize());
			map.put("roomProcessorID", roomprocessorId);
			rMap.put("RP", map);
			list.add(rMap);
		}
		return list;
	}
	
	

}
