package com.iflytek.roomprocessor.api.event;

import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.message.Event;
import com.iflytek.roomprocessor.net.message.Notify;

public interface IEventListener {
	
	public void roomEnterHandle(BaseEvent e,Notify notify,IConnection connection);
	
	public void roomLeaveHandle(BaseEvent e,Notify notify,IConnection connection);
	
	public void clientDisconnect(BaseEvent e,Notify notify,IConnection connection);
	
	public void userEnterOtherRoom(Event event);
	
	public void userKicked(Event event);

}
