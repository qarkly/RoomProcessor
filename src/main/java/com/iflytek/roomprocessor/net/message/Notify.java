package com.iflytek.roomprocessor.net.message;

import com.iflytek.roomprocessor.api.IMessage;
import com.iflytek.roomprocessor.net.message.MessageType.NotifyType;

public class Notify implements IMessage{

	private int t;
	private Header h;
	private Event d;
	
	public Event getD() {
		return d;
	}
	public void setD(Event d) {
		this.d = d;
	}
	
	public int getT() {
		return t;
	}
	public void setT(int t) {
		this.t = t;
	}
	public Header getH() {
		return h;
	}
	public void setH(Header h) {
		this.h = h;
	}

	@Override
	public String type() {
		if(d != null)
			return d.type();
		return NotifyType.UNKNOW;
	}
	
	
	@Override
	public void Print() {
		System.out.println(toString());
		
	}
	
	
}
