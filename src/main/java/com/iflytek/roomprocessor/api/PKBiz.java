package com.iflytek.roomprocessor.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.iflytek.roomprocessor.api.event.BaseEvent;
import com.iflytek.roomprocessor.api.event.IEventCallBackListener;
import com.iflytek.roomprocessor.api.event.IEventListener;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.components.asynctask.AsyncTask;
import com.iflytek.roomprocessor.global.Client;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.global.Room;
import com.iflytek.roomprocessor.global.UserInfo;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.conn.impl.RPConnManager;
import com.iflytek.roomprocessor.net.message.Event;
import com.iflytek.roomprocessor.net.message.MessageType.InCtrlName;
import com.iflytek.roomprocessor.net.message.Notify;
import com.iflytek.roomprocessor.net.message.StreamSend;
import com.iflytek.roomprocessor.net.message.MessageType.SendType;
import com.iflytek.roomprocessor.util.HttpUtil;
import com.iflytek.roomprocessor.util.JsonUtil;
import com.iflytek.roomprocessor.util.HttpUtil.HttpMethod;

public class PKBiz implements IEventListener {

	@SuppressWarnings("unchecked")
	@Override
	public void roomEnterHandle(BaseEvent e, Notify notify,
			IConnection connection) {
		System.out.println("KTVBiz----roomEnterHandle------消息："+notify.type());
		try {
			IEventCallBackListener listener = (IEventCallBackListener) e.getSource();
			String sid = notify.getH().getSid();
			Map<String, Object> msg = (Map<String, Object>) notify.getD().getMsg();
			String hashid = (String) ((Map<String, Object>)msg.get("u")).get("hid");
			String roomid = String.valueOf( ((Map<String, Object>)msg.get("r")).get("no"));
			
			
			HttpUtil http = new HttpUtil();
			http.setMethod(HttpMethod.GET);
//			http.addProperties("hashID", hashid);
			http.addProperties("roomID", roomid);
			http.addProperties("rightType","1");
			String url = Config.getInstance().getOperateInterface()+"/right/user/"+hashid+"/";
			http.sendRequest(url, "");
			String result = http.getResponse();
			
			Map<String, Object> map = (Map<String, Object>) JsonUtil.deserialize(result, Object.class);
			if("0000".equals(map.get("returnCode"))){
				listener.notifyRoomEntered(true, hashid, sid, roomid);
				
			}else {
				System.out.println(map.toString());
//				String returnCode  = (String) map.get("returnCode");
				notifyClient(sid, roomid, "0005", true, connection);
				listener.notifyRoomEntered(false, hashid, sid, roomid);
			}
		} catch (Exception e2) {
			System.out.println("PKBiz处理用户进入房间出现异常");
			e2.printStackTrace();
		}
		
		
		
	}
	
	/**
	 * 通知client成功进入房间或退出房间
	 * @param sId
	 * @param roomId
	 */
	private void notifyClient(String sId,String roomId,String returnCode,boolean isEnterRoom,IConnection connection){
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
		rt.put("cd", returnCode);
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

	@SuppressWarnings("unchecked")
	@Override
	public void roomLeaveHandle(BaseEvent e, Notify notify,
			IConnection connection) {
		System.out.println("PKBiz----roomLeaveHandle-----消息："+notify.type());
		try {
			IEventCallBackListener listener = (IEventCallBackListener) e.getSource();
			Map<String, Object> msg = (Map<String, Object>) notify.getD().getMsg();
			String sId = notify.getH().getSid();
			String roomId = String.valueOf(((Map<String, Object>)msg.get("r")).get("no"));
			UserInfo userInfo = RP.findSIdInUserInfo(sId);
			listener.notifyRoomLeft(true, userInfo.getHashId(), sId, roomId);
			
		} catch (Exception e2) {
			System.out.println("PKBiz处理用户离开房间，出现异常");
			e2.printStackTrace();
		}


	}

	@Override
	public void clientDisconnect(BaseEvent e, Notify notify,
			IConnection connection) {
		System.out.println("PKBiz---clientDisconnect---消息:"+notify.type());

	}



	@Override
	public void userEnterOtherRoom(Event event) {
		System.out.println("PKBiz --------userEnterOtherRoom--------消息："+event.type());
		try {
			Map<String, Object> msg = (Map<String, Object>) event.getMsg();
			System.out.println(msg.toString());
			String sId = (String) msg.get("sid");
			String roomId = String.valueOf( msg.get("rid"));
			String hashId = (String) msg.get("uid");
			UserInfo user = RP.findUserByHashId(hashId);
			if(user == null){
				System.out.println("没有该房间信息，hashid："+hashId);
				return;
			}
			List<Client> list = user.getClients();
			Room room = RP.findRoomByRoomId(roomId);
			for (Client client : list) {
				if(!client.getSid().equals(sId) && client.getRoomid().equals(roomId)){
					System.out.println(client.getSid());
					room.removeClient(client.getSid());
					IConnection connection = RPConnManager.getConnManager().getConnection(client.getClientId());
					notifyClient(client.getSid(), roomId, "1000", true, connection);
					UserInfo userInfo = RP.findUserByHashId(hashId);
					userInfo.removeClient(client.getSid());
					notifyConnector(client.getSid(), roomId, InCtrlName.DETACH_MESSAGE, connection);
					AsyncTask.getInstance().removeUserAsyncTask(hashId, client.getSid(), roomId);
				}
			}
		} catch (Exception e) {
			System.out.println("PKBiz处理userInOtherRoom消息出现异常");
			e.printStackTrace();
		}
		
		
	}

	private void notifyConnector(String sId,String roomId,String nm,IConnection connection){
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
			connection.flushOut();
		} catch (Throwable e) {
			System.out.println("PKBiz 通知connector出现异常");
			e.printStackTrace();
		}
		
		
	}
	@Override
	public void userKicked(Event event) {
		System.out.println("PKBiz --------userKicked--------消息："+event.type());
		
	}

}
