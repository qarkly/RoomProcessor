package com.iflytek.roomprocessor.net.message;

import com.iflytek.roomprocessor.api.IMessage;
import com.iflytek.roomprocessor.net.message.MessageType.NotifyType;

public class Event implements IMessage{
	
    

	private String nm;//消息类型
	private Object msg;//消息内容
	
	
	public Event(){
		super();
	}

	public Event(String nm, Object msg) {
		super();
		this.nm = nm;
		this.msg = msg;
	}

	public String getNm() {
		return nm;
	}

	public void setNm(String nm) {
		this.nm = nm;
	}

	



	@Override
	public void Print() {
		System.out.println(this.toString());
		
	}

	@Override
	public String toString() {
		String str = "{nm:"+nm+"msg:"+msg.toString()+"}";
		return str;
	}
	
	@Override
	public String type() {
		if(nm !=null)
			return nm;
		return NotifyType.UNKNOW;
	}

	public Object getMsg() {
		return msg;
	}

	public void setMsg(Object msg) {
		this.msg = msg;
	}
	
	

}
