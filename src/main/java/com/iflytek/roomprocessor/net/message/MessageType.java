package com.iflytek.roomprocessor.net.message;


public class MessageType {
	
	public  class NotifyType{
		public final static String ENTERROOM = "enterRoom";
		public final static String LEAVEROOM = "leaveRoom";
	    public final static String SOCKET_NAME = "SYS_SET_SOCKET_NAME";
	    public final static String CLIENT_DISCONNECT = "SYS_CLIENT_DISCONNECT";
		public final static String UNKNOW = "unknow";
		
	}
	
	public class SendType{
		public final static byte IN_CTRL = 0;
		public final static byte TO_CLIENT = 1;
		public final static byte TO_USER = 2;
		public final static byte TO_ROOM = 3;
		public final static byte TO_ALL = 4;
		public final static byte TO_ROOM_EXCEPT_ONE = 5;
		public final static byte TO_SCENE = 6;
	}
	
	public class InCtrlName{
		public final static String BACK_MESSAGE= "SYS_BACK_CLIENT_MESSAGE";
		public final static String DETACH_MESSAGE = "SYS_DETACH_ROOM_GROUP";
		public final static String ATTACH_MESSAGE = "SYS_ATTACH_ROOM_GROUP";
	}
	
	public class SubType{
		public final static String USER_IN_ROOM ="SYS_USER_IN_OTHER_ROOM";
		public final static String ROOM_CHANGED = "SYS_ROOM_INFO_CHANGED";
		public final static String KICK_USER = "GM_KICK_OUT_USER";
	}

}
