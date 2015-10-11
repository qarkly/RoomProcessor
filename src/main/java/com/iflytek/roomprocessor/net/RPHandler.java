package com.iflytek.roomprocessor.net;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.iflytek.roomprocessor.api.IHandler;
import com.iflytek.roomprocessor.api.IMessage;
import com.iflytek.roomprocessor.api.event.BaseEvent;
import com.iflytek.roomprocessor.api.event.IEventCallBackListener;
import com.iflytek.roomprocessor.api.event.IEventListener;
import com.iflytek.roomprocessor.classloading.ClassReloadHelper;
import com.iflytek.roomprocessor.common.exception.SysException;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.ThreadPool;
import com.iflytek.roomprocessor.components.asynctask.AsyncTask;
import com.iflytek.roomprocessor.components.rabbitmq.Publisher;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.global.Room;
import com.iflytek.roomprocessor.global.UserInfo;
import com.iflytek.roomprocessor.global.lock.LockAccess;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.message.MessageType.InCtrlName;
import com.iflytek.roomprocessor.net.message.MessageType.NotifyType;
import com.iflytek.roomprocessor.net.message.MessageType.SendType;
import com.iflytek.roomprocessor.net.message.MessageType.SubType;
import com.iflytek.roomprocessor.net.message.Event;
import com.iflytek.roomprocessor.net.message.Notify;
import com.iflytek.roomprocessor.net.message.StreamSend;
import com.iflytek.roomprocessor.net.room.RoomEnter;
import com.iflytek.roomprocessor.net.room.RoomLeave;

public class RPHandler implements IHandler,IEventCallBackListener {
	private final SysLogger logger;
	//sid对应的业务类型
	private final Map<String , Integer> sidToBiz = new HashMap<String, Integer>();
	
	private IConnection connection;
	
    private RoomEnter roomEnter;
	
	private RoomLeave roomLeave;
	
	public RPHandler(){
		roomEnter  = new RoomEnter();
		roomLeave = new RoomLeave();
		logger = new SysLogger(RPHandler.class.getName());
	}
	

	public RPHandler(IConnection connection) {
		super();
		this.connection = connection;
		roomEnter  = new RoomEnter();
		roomLeave = new RoomLeave();
		logger = new SysLogger(RPHandler.class.getName());
	}


	@Override
	public void handle(List<IMessage> list) {
		if(list == null || list.isEmpty())
			return;
		try {
			for (IMessage msg : list) {
				Notify notify = (Notify) msg;
				if(NotifyType.ENTERROOM.equals(notify.type())){
					enterRoom(notify);
				}else if (NotifyType.LEAVEROOM.equals(notify.type())) {
					leaveRoom(notify);
				} else if (NotifyType.CLIENT_DISCONNECT.equals(notify.type())) {
					clientDisconnect(notify);
				}else {
					logger.debug("处理业务消息:"+notify.toString());
					Integer bizType = sidToBiz.get(notify.getH().getSid());
					IEventListener listener = ClassReloadHelper.getInstance().getBizClass(bizType);
					if(listener == null)
						throw new SysException(String.format("没有找有对应的业务类，sid%s-bizType:%d", notify.getH().getSid(),bizType));
					Method method = listener.getClass().getDeclaredMethod(notify.type(), BaseEvent.class,Notify.class,IConnection.class);
				    method.invoke(listener, new BaseEvent(this),notify,connection);
				}
			}
		} catch (Throwable e) {
			logger.error("处理客户端消息出错，具体：", e);
		}
		

	}
	/**
	 * 框架处理用户进入房间消息，并通知业务类
	 * @param notify
	 *               客户端发送消息
	 * @see
	 *      <code>handle(List<IMessage> list) </code>方法
	 */
	private void enterRoom(Notify notify){
		logger.info("处理进入房间消息"+notify.toString());
		Room room = roomEnter.enterRoom(notify, connection);
		if(room != null){
			sidToBiz.put(notify.getH().getSid(), room.getBizType());
			IEventListener listener = ClassReloadHelper.getInstance().getBizClass(room.getBizType());
			listener.roomEnterHandle(new BaseEvent(this), notify, connection);
		}
	}
	
	/**
	 * 框架处理用户退出房间消息，并通知业务类
	 * @param notify
	 *               客户端发送消息
	 * @see
	 *      <code>handle(List<IMessage> list) </code>方法
	 */
	private void leaveRoom(Notify notify){
		logger.info("处理离开房间消息"+notify.toString());
		Room room = roomLeave.leaveRoom(notify, connection);
		if(room != null){
			IEventListener listener = ClassReloadHelper.getInstance().getBizClass(room.getBizType());
			listener.roomLeaveHandle(new BaseEvent(this), notify, connection);
		}
	}
	/**
	 * 框架处理用户断开连接消息，并通知业务类
	 * @param notify
	 *               客户端发送消息
	 * @see
	 *      <code>handle(List<IMessage> list) </code>方法
	 */
	@SuppressWarnings("unchecked")
	private void clientDisconnect(Notify notify){
		logger.info("处理用户断开消息");
		String sId = (String) ((Map<String, Object>)notify.getD().getMsg()).get("sid");
		Room room = RP.removeSIdFromRoom(sId);
		UserInfo userInfo = RP.findSIdInUserInfo(sId);
		Lock lock = LockAccess.getLockByName(userInfo.getHashId());
		lock.lock();
		try {
			if(userInfo != null){
				userInfo.removeClient(sId);
			}
			
		} finally{
			lock.unlock();
		}
		IEventListener listener = ClassReloadHelper.getInstance().getBizClass(room.getBizType());
		listener.clientDisconnect(new BaseEvent(this), notify, connection);
		if(userInfo != null){
			AsyncTask.getInstance().removeUserAsyncTask(userInfo.getHashId(), sId, room.getRoomId());
		}
		
	}
	

	public IConnection getConnection() {
		return connection;
	}

	public void setConnection(IConnection connection) {
		this.connection = connection;
		
	}

	@Override
	public void notifyRoomEntered(boolean isSuccess, String hashId, String sId,
			String roomId) {
		if(isSuccess){
			notifyConnector(sId,roomId,InCtrlName.ATTACH_MESSAGE);
			notifyClient(sId,roomId,true);
			notifyRoomProcessors(sId,hashId, roomId);
			AsyncTask.getInstance().addUserAsyncTask(sId, hashId, roomId);
		}else {
			logger.info(String.format("hashid:%s用户sid:%s进入房间%s失败", hashId,sId,roomId));
			sidToBiz.remove(sId);
			removeUser(hashId, sId);
			Room room = RP.findRoomByRoomId(roomId);
			if(room != null)
			   room.removeClient(sId);
		}
		
	}
	
	/**
	 * 通知client成功进入房间或退出房间
	 * @param sId
	 * @param roomId
	 */
	private void notifyClient(String sId,String roomId,boolean isEnterRoom){
		//StreamSend t:{},tar:{},d:{}
		StreamSend send = new StreamSend(SendType.TO_CLIENT);
		send.setTar(sId);
		//构造Event两个部分nm:{},msg:{}
		Event event = new Event();
		Map<String, Object> msg = new LinkedHashMap<String, Object>();
		Map<String, Object> r = new LinkedHashMap<String, Object>();
		Map<String, Object> rt = new LinkedHashMap<String, Object>();
		if(isEnterRoom){
			event.setNm("roomEntered");
			Map<String, Object> rd5 = new LinkedHashMap<String, Object>();
			rd5.put("id", RP.findRoomByRoomId(roomId).getRed5Id());
			msg.put("rd5", rd5);
		}else {
			event.setNm("roomLeft");
		}
		rt.put("cd", "0000");
		r.put("no", Integer.valueOf(roomId));
		msg.put("rt", rt);
		msg.put("r", r);
		event.setMsg(msg);
		//event构造完毕
		send.setD(event);
		//send 构造完毕
		connection.appendOut(send);
		connection.flushOut();
	}
	/**
	 * 通知connector 指定的sid进入房间
	 * @param sId
	 *         用户的sId
	 * @param roomId
	 *         房间的roomid
	 * @param nm
	 *        内部控制消息名
	 */
	private void notifyConnector(String sId,String roomId,String nm){
		try {
			StreamSend send = new StreamSend(SendType.IN_CTRL);
			send.setTar("");
			Event dEvent = new Event();
			dEvent.setNm(nm);
			Map<String, Object> msg = new LinkedHashMap<String, Object>();
			msg.put("sid", sId);
			msg.put("rid", Integer.valueOf(roomId));
			dEvent.setMsg(msg);
			send.setD(dEvent);
			connection.appendOut(send);
		} catch (Throwable e) {
			logger.error("通知Connector出错，具体：", e);
		}
		
		
	}
	/**
	 * 向所有的roomprocessor进程pub用户进入房间消息
	 * @param hashId
	 * @param roomId
	 */
	private void notifyRoomProcessors(final String sId,final String hashId,final String roomId){
		ThreadPool.getSingleThreadExecutor().submit(new Runnable() {
			
			@Override
			public void run() {
				try {
					Event userInRoom = new Event();
					userInRoom.setNm(SubType.USER_IN_ROOM);
					Map<String, Object> map = new LinkedHashMap<String, Object>();
					map.put("sid",sId );
					map.put("uid", hashId);
					map.put("rid", Integer.valueOf(roomId));
					Room room = RP.findRoomByRoomId(roomId);
					map.put("biz", room.getBizType());
					userInRoom.setMsg(map);
					Publisher.publish(userInRoom);
				} catch (Throwable e) {
					logger.error("通知其他roomprocessor用户进入房间失败，具体：", e);
				}
				
				
			}
		});
	}
	
	/**
	 * 从用户中移除sid的信息
	 * @param hashId
	 * @param sId
	 */
	private void removeUser(String hashId,String sId){
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


	@Override
	public void notifyRoomLeft(boolean isSuccess, String hashId, String sId,
			String roomId) {
		try {
			if(!isSuccess)
				return;
			sidToBiz.remove(sId);
			removeClient(roomId, sId);
			removeUser(hashId, sId);
			notifyConnector(sId, roomId, InCtrlName.DETACH_MESSAGE);
			notifyClient(sId, roomId, false);
			AsyncTask.getInstance().removeUserAsyncTask(hashId, sId, roomId);
		} catch (Throwable e) {
			logger.error("处理leaveRoom回调出错，具体：", e);
		}
		
		
	}
   // 将用户的sid消息用房间用户信息中移除
	private void removeClient(String roomId,String sId){
		Room room = RP.findRoomByRoomId(roomId);
		room.removeClient(sId);
		room.setLastLeftTime(System.currentTimeMillis());
	}
	
   
	

}
