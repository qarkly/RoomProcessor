package com.iflytek.roomprocessor.api.event;

import java.util.EventObject;

public class BaseEvent extends EventObject{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8790642357061853471L;

	public BaseEvent(Object source) {
		super(source);
	}

}
