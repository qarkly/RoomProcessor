package com.iflytek.roomprocessor.net.message;

import com.iflytek.roomprocessor.api.IMessage;

public class StreamSend implements IMessage{
	
	private int t;
	private String tar;
	private Event d;
	
	public StreamSend() {
		super();
	}
	
	

	public StreamSend(int t) {
		super();
		this.t = t;
	}



	public StreamSend(int t, String tar, Event d) {
		super();
		this.t = t;
		this.tar = tar;
		this.d = d;
	}

	@Override
	public String type() {
		if (d != null) {
			return d.type();
		}
		return "unknow";
	}

	@Override
	public void Print() {
		System.out.println(toString());
		
	}

	public int getT() {
		return t;
	}

	public void setT(int t) {
		this.t = t;
	}

	public String getTar() {
		return tar;
	}

	public void setTar(String tar) {
		this.tar = tar;
	}

	public Event getD() {
		return d;
	}

	public void setD(Event d) {
		this.d = d;
	}

}
