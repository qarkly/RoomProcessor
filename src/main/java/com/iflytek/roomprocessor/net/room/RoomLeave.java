package com.iflytek.roomprocessor.net.room;

import java.util.LinkedHashMap;
import java.util.Map;

import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.global.Client;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.global.Room;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.message.Event;
import com.iflytek.roomprocessor.net.message.Notify;
import com.iflytek.roomprocessor.net.message.StreamSend;
import com.iflytek.roomprocessor.net.message.MessageType.SendType;

public class RoomLeave {
	private static final String RoomNoExisted = "0001";//房间不存在
	private static final String UserNotInRoom = "0008";//用户不在房间内
	private static final SysLogger logger = new SysLogger(RoomLeave.class.getName());
	@SuppressWarnings("unchecked")
	public Room leaveRoom(Notify notify,IConnection conn){
		try {
			Map<String, Object> map = (Map<String, Object>) notify.getD().getMsg();
			String sId = notify.getH().getSid();
			String roomId = String.valueOf(((Map<String, Object>)map.get("r")).get("no"));
			Room room = RP.findRoomByRoomId(roomId);
			if(room == null){
				notifyException(RoomNoExisted, sId, roomId, conn);
				return null;
			}
			Client client = room.getClient(sId);
			if(client == null){
				notifyException(UserNotInRoom, sId, roomId, conn);
				return null;
			}
			return room;
			
		} catch (Throwable e) {
			logger.error("处理leaveRoom消息出错，具体：", e);
		}
		return null;
	}

	private void notifyException(String retCode,String sId,String roomId,IConnection connection){
		//StreamSend t:{},tar:{},d:{}
				StreamSend send = new StreamSend(SendType.TO_CLIENT);
				send.setTar(sId);
				//构造Event两个部分nm:{},msg:{}
				Event event = new Event();
				event.setNm("roomLeft");
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
	
}
