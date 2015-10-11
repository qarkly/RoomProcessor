package com.iflytek.roomprocessor.components.rabbitmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.iflytek.roomprocessor.api.event.IEventListener;
import com.iflytek.roomprocessor.classloading.ClassReloadHelper;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.asynctask.AsyncTask;
import com.iflytek.roomprocessor.global.Client;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.global.Room;
import com.iflytek.roomprocessor.global.TargetSend;
import com.iflytek.roomprocessor.global.UserInfo;
import com.iflytek.roomprocessor.global.lock.LockAccess;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.conn.IRPConnManager;
import com.iflytek.roomprocessor.net.conn.impl.RPConnManager;
import com.iflytek.roomprocessor.net.message.Event;
import com.iflytek.roomprocessor.net.message.StreamSend;
import com.iflytek.roomprocessor.net.message.MessageType.InCtrlName;
import com.iflytek.roomprocessor.net.message.MessageType.SendType;
import com.iflytek.roomprocessor.net.message.MessageType.SubType;
import com.iflytek.roomprocessor.util.JsonUtil;
import com.iflytek.roomprocessor.zookeeper.ZooKeeperWrapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

public class Subscriber extends Thread {
	private final static SysLogger logger = new SysLogger(Subscriber.class.getName());
	private static Connection connection = null;
	private static ConnectionFactory factory = null;
	private static Object object = new Object();
	private  final String ExChange ;
	private volatile boolean closed = false;
	private  Channel channel = null;
	
	private static void connect(){
		if(factory == null){
			Config config = Config.getInstance();
			 factory = new ConnectionFactory();
			factory.setHost(config.getRabbitMqUrl());
			factory.setPort(config.getRabbitMqPort());
		}
		while (true) {
			try {
				connection = factory.newConnection();
				break;
			} catch (IOException e) {
				logger.error("创建rabbitmq连接失败,具体:", e);
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {}
			
		}
		connection.addShutdownListener(new ShutdownListener() {
				
				@Override
				public void shutdownCompleted(ShutdownSignalException arg0) {
					logger.debug("捕捉到ShutdownSignalException，重新connect");
					synchronized (object) {
						if(!connection.isOpen())
							connect();
					}
				}
		});
	}

	public Subscriber(String exChange) {
		super();
		ExChange = exChange;
	}
	
	public void close(){
		closed = true;
		try {
			channel.close();
			connection.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void run() {
		synchronized (object) {
			if(connection == null || !connection.isOpen())
				connect();
		}
		QueueingConsumer consumer = null;
		try{
			channel = connection.createChannel();
			channel.exchangeDeclare(ExChange, "fanout");
			String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(queueName, ExChange, "");
			consumer = new QueueingConsumer(channel);
			channel.basicConsume(queueName, true, consumer);
		} catch (IOException e) {
			logger.error("创建channel失败,具体:", e);
			return;
		}
		while (!closed) {
			try {
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				String message = new String(delivery.getBody(), "utf-8");
				handleMessage(message);
			} catch (InterruptedException e) {
				logger.debug("获取订阅消息被中断");
				Thread.currentThread().interrupt();
			} catch (ShutdownSignalException e) {
				logger.info("订阅rabbitmq被关闭,Exchange:"+ExChange);
				run();
				break;
			} catch (Throwable e) {
				logger.error("获取订阅消息出现异常，具体：", e);
				break;
			}
		}
	}

	/**
	 * 处理订阅消息，三种订阅消息：
	 * 1.用户进入房间
	 * 2.房间信息变更
	 * 3.踢出用户
	 * @param message
	 */
	private void handleMessage(String message) {
		logger.debug("Exchange:"+ExChange+"取得订阅消息,内容:" + message);
		Event event = (Event) JsonUtil.deserialize(message, Event.class);
		if (SubType.USER_IN_ROOM.equals(event.getNm())) {
			enterRoom(event);
			return;
		}
		if (SubType.ROOM_CHANGED.equals(event.getNm())) {
			roomChange(event);
			return;
		}
		if (SubType.KICK_USER.equals(event.getNm())) {
			kickUser(event);
			return;
		}
		logger.error("Exchange:"+ExChange+"未知的订阅消息,具体:" + message);
	}
    /**
     * 收到用户进入其他房间信息，通知所有的业务类做处理
     * @param event
     */
	private void enterRoom(Event event) {
        List<IEventListener> list = ClassReloadHelper.getInstance().getAllBizClasses();
        for (IEventListener iEventListener : list) {
			iEventListener.userEnterOtherRoom(event);
		}
		
	}

	/**
	 * 处理房间信息变更消息
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	private void roomChange(Event event) {
		
		try {
			Map<String, Object> map = (Map<String, Object>) event.getMsg();
			Integer roomId = (Integer) map.get("rid");
			Room room = RP.findRoomByRoomId(String.valueOf(roomId));
			if (room == null)
				return;
			String rno = (String) map.get("rno");
			if(rno != null){
				room.setRoomBizNo(rno);
			}
			Integer biz = (Integer) map.get("biz");
			if(biz != null){
				room.setBizType(biz);
			}
			String nm = (String) map.get("nm");
			if(nm != null){
				room.setRoomName(nm);
			}
			Integer max = (Integer) map.get("max");
			if(max != null){
				room.setMaxUserCount(max);
			}
			List<Integer> chs = (List<Integer>) map.get("chs");
			if(chs != null){
				room.setChannelNos(chs);
			}
			String red5 = (String) map.get("rd5");
			if(red5 != null){
		    	room.setRed5Id(red5);
			}
			//更新room节点信息
			ZooKeeperWrapper wrapper = ZooKeeperWrapper.getInstance();
			Map<String, Object> roomnode = new HashMap<String, Object>();
			roomnode.put("maxUserCount", room.getMaxUserCount());
			roomnode.put("roomName", room.getRoomName());
			String json = JsonUtil.serialize(roomnode, Object.class);
			wrapper.updateRoom(String.valueOf(roomId), json);
		} catch (Throwable e) {
			logger.error("变更房间信息出错,具体:", e);
		}
	}

	/**
	 * 处理踢出用户消息,消息中包含rid把用户从某一房间踢出，不包含rid把用户从所有的房间中踢出
	 * @param event
	 */
	private void kickUser(Event event) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) event.getMsg();
			String hashId = (String)map.get("uid");
			Integer tm = (Integer)map.get("tm");
			Integer roomId = (Integer)map.get("rid");
			notifyUserKicked(event,roomId);
			if(roomId != null){
				forbiddenInRoom(hashId, roomId, tm);
			}else {
				fobiddenInAllRoom(hashId,tm);
			}
		} catch (Throwable e) {
			logger.error("踢出用户出错，具体：", e);
		}
	}
	/**
	 * 通知所有的业务,用户被踢
	 * @param event
	 */
	private void notifyUserKicked(Event event,Integer roomId){
		if(roomId != null){
			Room room = RP.findRoomByRoomId(String.valueOf(roomId));
			IEventListener listener = ClassReloadHelper.getInstance().getBizClass(room.getBizType());
			listener.userKicked(event);
		}else {
			List<IEventListener> list = ClassReloadHelper.getInstance().getAllBizClasses();
			for (IEventListener iEventListener : list) {
				iEventListener.userKicked(event);
			}
		}
	}
	/**
	 * 把用户从某一房间踢出
	 */
	private void forbiddenInRoom(String hashId,Integer roomId,Integer tm){
		UserInfo userInfo = RP.findUserByHashId(hashId);
		Room room = RP.findRoomByRoomId(String.valueOf(roomId));
		if(userInfo == null || room == null)
			return;
		logger.info(String.format("将用户hashId：%s从房间roomId：%s中踢出", hashId,room.getRoomId()));
		Lock lock = LockAccess.getLockByName(hashId);
		lock.lock();
		try{
			List<Client> list = userInfo.getClients();
			for (Client client : list) {
				Client oldClient = room.removeClient(client.getSid());
				if(oldClient != null){
					logger.debug(String.format("将用户hashID：%s-sId：%s从房间roomId：%s踢出", hashId,client.getSid(),roomId));
					room.setLastLeftTime(System.currentTimeMillis());
					sendDetachRoom(oldClient, roomId);
					notifyClient(tm,oldClient);
					userInfo.removeClient(oldClient.getSid());
					AsyncTask.getInstance().removeUserAsyncTask(oldClient.getHashid(), oldClient.getSid(), String.valueOf(roomId));
				}
					
			}
		}finally{
			lock.unlock();
		}
	}
	/**
	 * 将用户从所有的房间踢出
	 */
	private void fobiddenInAllRoom(String hashId,Integer tm){
		UserInfo userInfo = RP.findUserByHashId(hashId);
		if(userInfo == null)
			return;
		logger.info("将用户hashId："+hashId+"从所有房间中踢出");
		Lock lock = LockAccess.getLockByName(hashId);
		lock.lock();
		try{
			List<Client> list = userInfo.getClients();
			notifyUser(tm,hashId);
			for (Client client : list) {
				Room room = RP.removeSIdFromRoom(client.getSid());
				if(room != null){
					room.setLastLeftTime(System.currentTimeMillis());
					sendDetachRoom(client, Integer.valueOf(room.getRoomId()));
					AsyncTask.getInstance().removeUserAsyncTask(client.getHashid(), client.getSid(), room.getRoomId());
				}
			}
			RP.removeUser(userInfo);
		}finally{
			lock.unlock();
		}
		
	}
	/**
	 * 发送TO_USER消息，内容：禁止用户进入房间时长
	 * @param tm
	 * @param hashId
	 * @param client
	 */
	private void notifyUser(Integer tm,String hashId){
		
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Map<String, Object> tmap = new LinkedHashMap<String, Object>();
		tmap.put("tm", tm);
		map.put("frm", tmap);
		TargetSend.sendToUser(hashId, "notifyForbiddenIntoRoom", map);
		
	}
	
	/**
	 * 发送TO_CLIENT消息，内容：禁止用户进入房间时长
	 * @param tm
	 * @param client
	 */
	private void notifyClient(Integer tm,Client client){
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Map<String, Object> tmap = new LinkedHashMap<String, Object>();
		tmap.put("tm", tm);
		map.put("frm", tmap);
		
		TargetSend.sendToClient(client.getSid(), "notifyForbiddenIntoRoom", map);
		
		
	}
	/**
	 * 发送用户离开房间内部控制消息
	 * 
	 */
	private void sendDetachRoom(Client client,Integer roomId){
		try {
			StreamSend send = new StreamSend(SendType.IN_CTRL);
			send.setTar("");
			Event dEvent = new Event();
			dEvent.setNm(InCtrlName.DETACH_MESSAGE);
			Map<String, Object> msg = new LinkedHashMap<String, Object>();
			msg.put("sid", client.getSid());
			msg.put("rid", roomId);
			dEvent.setMsg(msg);
			send.setD(dEvent);
			IRPConnManager manager = RPConnManager.getConnManager();
			IConnection connection = manager.getConnection(client.getClientId());
			connection.appendOut(send);
		} catch (Throwable e) {
			logger.error("通知Connector出错，具体：", e);
		}
		
		
	}

}
