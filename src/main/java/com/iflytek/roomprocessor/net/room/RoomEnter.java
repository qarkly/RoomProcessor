package com.iflytek.roomprocessor.net.room;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.exception.SysException;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.asynctask.AsyncTask;
import com.iflytek.roomprocessor.global.Client;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.global.Room;
import com.iflytek.roomprocessor.global.UserInfo;
import com.iflytek.roomprocessor.global.lock.LockAccess;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.message.Event;
import com.iflytek.roomprocessor.net.message.Notify;
import com.iflytek.roomprocessor.net.message.StreamSend;
import com.iflytek.roomprocessor.net.message.MessageType.InCtrlName;
import com.iflytek.roomprocessor.net.message.MessageType.SendType;
import com.iflytek.roomprocessor.util.HttpUtil;
import com.iflytek.roomprocessor.util.JsonUtil;
import com.iflytek.roomprocessor.util.HttpUtil.HttpMethod;
import com.iflytek.roomprocessor.zookeeper.ZooKeeperWrapper;

public class RoomEnter {
	private static SysLogger logger = new SysLogger(RoomEnter.class.getName());
	private static final String requestParam = "RoomIndexID";
	private static final String Success = "0000";
	private static final String NoExist = "2200";
	private static final String RoomNoExisted = "0001";
	private static final String RoomIsFull = "0002";
	private static final String SIdReLogin = "0004";
	@SuppressWarnings("unchecked")
	public Room enterRoom(Notify notify,IConnection connection){
		logger.info("处理进入房间消息");
		String hashId = null,sId = null;
		try {
			Map<String, Object> map = (Map<String, Object>) notify.getD().getMsg();
			String roomid =String.valueOf(((Map<String, Object>)map.get("r")).get("no"));
		    sId = notify.getH().getSid();
			hashId = (String) ((Map<String, Object>)map.get("u")).get("hid");
		    Room room = RP.findRoomByRoomId(roomid);
		    Client client = new Client(sId, notify.getH().getIp(), connection.getClinetId());
		    client.setRoomid(roomid);
		    client.setHashid(hashId);
		    if(!addUser(hashId, client, roomid, connection))
		    	return null;
		    if(room == null){
		    	return roomNoExisted(client,hashId,roomid, notify, connection);
		    }else {
				return roomExisted( room,hashId,client, connection);
			}
			
		} catch (Throwable e) {
			logger.error("处理enterroom消息出错，具体：", e);
			removeUser(hashId, sId);
			if(e instanceof SysException)
			   rollbackMessage(notify, connection);
		}
		return null;
		
	}
	
	
	/**
	 * 用户进入房间，先将client信息添加到用户信息中，由业务处理之后回调RPHandler，通知用户进入房间是否成功
	 * @param hashId
	 * @param client
	 * @param roomId
	 * @param connection
	 * @return
	 *         true---添加成功
	 *         false---用户sid重复登录，或者用户hashid不存在
	 */
	private boolean  addUser(String hashId,Client client,String roomId,IConnection connection){
		Lock lock = LockAccess.getLockByName(hashId);
		lock.lock();
		try{
			UserInfo user = RP.findUserByHashId(hashId);
			if(user != null){
				Client oldClient = user.putClient(client);
				if(oldClient != null){
					logger.info("该用户sid重复登录");
					notifyRetCode(SIdReLogin, client.getSid(), roomId, connection);
					return false;
				}
				return true;
			}else {
				logger.info("用户hashid不存在，将新建用户信息！");
				user = new UserInfo();
				user.setHashId(hashId);
				user.putClient(client);
				RP.addUser(user);
				return true;
			}
			
		}finally{
			lock.unlock();
		}
	}
	/**
	 * 用户进入出现异常后，移除用户信息
	 * @param hashId
	 * @param sId
	 * 
	 * @see 
	 *      enterRoom方法
	 * 
	 */
	private void removeUser(String hashId,String sId){
		if(hashId == null || sId == null)
			return;
		Lock lock = LockAccess.getLockByName(hashId);
		lock.lock();
		try {
			logger.info("移除hashid:"+hashId+"用户的sid:"+sId);
			UserInfo user = RP.findUserByHashId(hashId);
			user.removeClient(sId);
			if(user.isEmpty()){
				logger.info("hashid:"+hashId+"用户sid为空，将移除用户信息！");
				RP.removeUser(user);
			}
		} finally{
			lock.unlock();
		}
	}
	
	
	/**
	 * 用户进入的房间内存中不存在，通过http方法查询房间的信息，新建房间
	 * @param client
	 * @param hashId
	 * @param roomid
	 * @param notify
	 * @param connection
	 * @return
	 *         Room
	 */
	private Room roomNoExisted(Client client,String hashId,String roomid,Notify notify,IConnection connection){
		Map<String, Object> roomMap = queryRoomInfo(roomid);
    	String returnCode = (String) roomMap.get("ReturnCode");
    	if(Success.equals(returnCode)){
    		Room room =  createRoom(notify, connection, roomid, roomMap);
    		room.addClient(client);
    		AsyncTask.getInstance().addNewRoomAsyncTask(room);
    		return room;
    	}else if (NoExist.equals(returnCode)) {
    		removeUser(hashId, client.getSid());
    		notifyRetCode(RoomNoExisted,client.getSid(), roomid, connection);
    		return null;
    	}else {
    		removeUser(hashId, client.getSid());
    		logger.error("调用查询房间信息webAPI遇到异常,返回码:"+returnCode);
    		return null;
    	}
	}
	/**
	 * 房间在内存中存在，先判断房间是否已满再将client信息添加到房间信息下
	 * @param room
	 * @param hashId
	 * @param client
	 * @param connection
	 * @return
	 */
	private Room roomExisted(Room room,String hashId,Client client,IConnection connection){
		Lock lock = LockAccess.getLockByName(room.getRoomId());
		lock.lock();
		try {
			if(room.isFull()){
				notifyRetCode(RoomIsFull, client.getSid(), room.getRoomId(), connection);
				removeUser(hashId, client.getSid());
				return null;
			}else {
				room.addClient(client);
				return room;
			}
		} finally{
			lock.unlock();
		}
	}
	
	private void rollbackMessage(Notify notify ,IConnection connection){
		
		StreamSend send = new StreamSend();
		send.setT(SendType.IN_CTRL);
		send.setTar("");
		Event sendEvent = new Event();
		sendEvent.setNm(InCtrlName.BACK_MESSAGE);
		Map<String, Object> msg = new LinkedHashMap<String, Object>();
		msg.put("sid", notify.getH().getSid());
		msg.put("nm", notify.getD().getNm());
		msg.put("msg", notify.getD().getMsg());
		sendEvent.setMsg(msg);
		send.setD(sendEvent);
		
		connection.appendOut(send);
		connection.flushOut();
	}
	
	/**
	 * 框架对进入房间消息处理异常
	 * @param retCode 返回码：0001：{房间不存在} 0002：{房间人满} 0004：{sid重复登录}
	 * @param sId
	 * @param roomId
	 * @param connection
	 */
	private void notifyRetCode(String retCode,String sId,String roomId,IConnection connection){
		//StreamSend t:{},tar:{},d:{}
		StreamSend send = new StreamSend(SendType.TO_CLIENT);
		send.setTar(sId);
		//构造Event两个部分nm:{},msg:{}
		Event event = new Event();
		event.setNm("roomEntered");
		Map<String, Object> msg = new LinkedHashMap<String, Object>();
		Map<String, Object> r = new LinkedHashMap<String, Object>();
		Map<String, Object> rt = new LinkedHashMap<String, Object>();
		rt.put("cd", retCode);
		r.put("no", roomId);
		msg.put("rt", rt);
		msg.put("r", r);
		event.setMsg(msg);
		//event构造完毕
		send.setD(event);
		//send 构造完毕
		connection.appendOut(send);
		connection.flushOut();
	}
	
	@SuppressWarnings("unchecked")
	private Room createRoom(Notify notify, IConnection connection,String roomId,Map<String, Object> roomMap){
		Lock lock = LockAccess.getLockByName(roomId);
		lock.lock();
    	try{
    		Room room = RP.findRoomByRoomId(roomId);
    		Integer maxUserCount = (Integer)roomMap.get("LimitCount");
        	String roomName = (String) roomMap.get("Name");
    		if(room != null)
    			return room;
    		ZooKeeperWrapper zooKeeperWrapper = ZooKeeperWrapper.getInstance();
        	if(!zooKeeperWrapper.isStarted())
        		throw new SysException("zookeeper 连接断开");
        	zooKeeperWrapper.createRoom(roomId);
        	Map<String, Object> map = new LinkedHashMap<String, Object>();
        	map.put("maxUserCount", maxUserCount);
        	map.put("roomName", roomName);
        	String json = JsonUtil.serialize(map, Object.class);
        	zooKeeperWrapper.updateRoom(roomId, json);
        	if(!zooKeeperWrapper.createRoomChildRPNode(roomId)){
        		logger.info("房间roomprocesser节点已经被创建，进入消息回退");				
        		rollbackMessage(notify, connection);
        		return null;
        	}
        	logger.debug(roomMap.toString());
        	List<Integer> list =  (List<Integer>) roomMap.get("ChannelNos");
        	int roomType = (Integer) roomMap.get("RoomType");
        	int bizType = (Integer)roomMap.get("bizTypeNo");
        	room = new Room(roomId, maxUserCount, roomName, list, roomType,bizType);
        	room.setLastLeftTime(System.currentTimeMillis());
        	Map<String, Object> dmap = (Map<String, Object>) notify.getD().getMsg();
			String red5Id =(String) ((Map<String, Object>)((Map<String, Object>)dmap).get("rd5")).get("id");
        	room.setRed5Id(red5Id);
        	RP.addRoom(room);
        	return room;
    	}finally{
    		lock.unlock();
    	}
    	
	}
	
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> queryRoomInfo(String roomid){
		Config config = Config.getInstance();
		HttpUtil httpRequest = new HttpUtil();
		httpRequest.setTimeout(config.getHttpTimeOut());
		httpRequest.setMethod(HttpMethod.GET);
		httpRequest.addProperties(requestParam, roomid);
		httpRequest.sendRequest(config.getQueryRoominfoUrl()+"/Room/SearchRoomInfo", "");
		String response = httpRequest.getResponse();
		logger.debug(response);
		if(response.isEmpty())
			return null;
		Map<String, Object> map = (Map<String, Object>) JsonUtil.deserialize(response, Map.class);
		logger.debug(map.toString());
		Map<String, Object> result = new HashMap<String, Object>();
		Object bool = map.get("Success");
		result.put("Success", bool);
		result.put("ReturnCode", map.get("ReturnCode"));
		if(bool.equals(new Boolean(true))){
			Map<String, Object> roomDetail = (Map<String, Object>) ((Map<String, Object>)((Map<String, Object>)map.get("Data")).get("roomDetail")).get("liveRoomIndex");
			result.put("ChannelNos", roomDetail.get("ChannelNos"));
			result.put("ID", roomDetail.get("ID"));
			result.put("Name", roomDetail.get("Name"));
			result.put("RoomType", roomDetail.get("RoomType")); 
			result.put("LimitCount", roomDetail.get("LimitCount"));
			result.put("bizTypeNo", roomDetail.get("BizTypeNo"));
		}
		return result;
	}

	
}
