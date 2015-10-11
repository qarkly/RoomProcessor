package com.iflytek.roomprocessor.api;

import java.util.Map;

import com.iflytek.roomprocessor.api.event.BaseEvent;
import com.iflytek.roomprocessor.api.event.IEventCallBackListener;
import com.iflytek.roomprocessor.api.event.IEventListener;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.global.UserInfo;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.message.Event;
import com.iflytek.roomprocessor.net.message.Notify;

public class KTVBiz implements IEventListener {

	@SuppressWarnings("unchecked")
	@Override
	public void roomEnterHandle(BaseEvent e, Notify notify,
			IConnection connection) {
		System.out.println("KTVBiz----roomEnterHandle------消息："+notify.type());
		IEventCallBackListener listener = (IEventCallBackListener) e.getSource();
		String sid = notify.getH().getSid();
		Map<String, Object> msg = (Map<String, Object>) notify.getD().getMsg();
		String hashid = (String) ((Map<String, Object>)msg.get("u")).get("hid");
		String roomid = (String) ((Map<String, Object>)msg.get("r")).get("no");
		listener.notifyRoomEntered(true, hashid, sid, roomid);

	}

	@SuppressWarnings("unchecked")
	@Override
	public void roomLeaveHandle(BaseEvent e, Notify notify,
			IConnection connection) {
		System.out.println("KTVBiz----roomLeaveHandle-----消息："+notify.type());
		IEventCallBackListener listener = (IEventCallBackListener) e.getSource();
		Map<String, Object> msg = (Map<String, Object>) notify.getD().getMsg();
		String sId = notify.getH().getSid();
		String roomId = (String) ((Map<String, Object>)msg.get("r")).get("no");
		UserInfo userInfo = RP.findSIdInUserInfo(sId);
		listener.notifyRoomLeft(true, userInfo.getHashId(), sId, roomId);

	}

	@Override
	public void clientDisconnect(BaseEvent e, Notify notify,
			IConnection connection) {
		System.out.println("KTVBiz---clientDisconnect---消息:"+notify.type());

	}



	@Override
	public void userEnterOtherRoom(Event event) {
		System.out.println("KTVBiz --------userEnterOtherRoom--------消息："+event.type());
		
	}

	@Override
	public void userKicked(Event event) {
		System.out.println("KTVBiz --------userKicked--------消息："+event.type());
		
	}

}
