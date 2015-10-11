package com.iflytek.roomprocessor.net;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;

import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.conn.IRPConnManager;
import com.iflytek.roomprocessor.net.conn.impl.RPConnManager;
import com.iflytek.roomprocessor.net.conn.impl.RPConnection;
import com.iflytek.roomprocessor.net.filter.NotifyFilter;
import com.iflytek.roomprocessor.net.message.MessageType.NotifyType;
import com.iflytek.roomprocessor.net.message.Notify;

public class RPMinaIoHandler extends IoHandlerAdapter {

	private static SysLogger logger  = new SysLogger(RPMinaIoHandler.class.getName());
	private static final String STATE = "state";
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		logger.debug("新连接session被创建");
		TextLineCodecFactory codecFactory = new TextLineCodecFactory(Charset.forName("utf-8"));
		codecFactory.setDecoderMaxLineLength(Integer.MAX_VALUE);
		codecFactory.setEncoderMaxLineLength(Integer.MAX_VALUE);
		session.getFilterChain().addLast("protocolFilter", new ProtocolCodecFilter(codecFactory));
		session.getFilterChain().addLast("notifyFilter", new NotifyFilter());
		if(logger.isDebugEnabled()){
			session.getFilterChain().addLast("logger", new LoggingFilter());
		}
		session.setAttribute(STATE);
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		logger.warn(String.format("Exception caught :%s", cause.getMessage()));
//		if(logger.isDebugEnabled())
//			logger.error("Exception detail", cause);
	}
	
	private void socketNameReceived(Notify notify,IoSession session){
		if(!NotifyType.SOCKET_NAME.equals(notify.type())){
			logger.error("客户端发送消息非法，具体："+notify.type());
			session.close(true);
			return;
		}
		@SuppressWarnings("unchecked")
		String clientId = (String) ((Map<String, Object>)notify.getD().getMsg()).get("nm");
		IRPConnManager manager = RPConnManager.getConnManager();
		IConnection connection = manager.getConnection(clientId);
		if(connection == null){
			connection = manager.createConnection(clientId);
			connection.setIoSession(session);
			session.removeAttribute(STATE);
			session.setAttribute(RPConnection.RP_CONNECTION_KEY, connection);
		}else {
			connection.setIoSession(session);
			connection.continues();
			session.removeAttribute(STATE);
			session.setAttribute(RPConnection.RP_CONNECTION_KEY, connection);
		}
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		Object object = session.getAttribute(STATE);
		Notify notify = (Notify) message;
		logger.debug("收到客户端发送消息,type:"+notify.type());
		if(object != null){
			socketNameReceived(notify, session);
		}else {
			IConnection connection = (IConnection) session.getAttribute(RPConnection.RP_CONNECTION_KEY);
			connection.appendIn(notify);
		}
		
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
        logger.debug("消息发送成功");
		super.messageSent(session, message);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		logger.debug("连接断开");
		IConnection connection = (IConnection) session.getAttribute(RPConnection.RP_CONNECTION_KEY);
		if(connection != null){
			connection.pause();
			connection.removeIoSession();
			session.removeAttribute(RPConnection.RP_CONNECTION_KEY);
			logger.debug(String.format("远程连接IP:%s Port:%d 断开", connection.getRemoteAddress(),connection.getRemotePort()));
		}
		
	}


	@Override
	public void sessionOpened(IoSession session) throws Exception {
		super.sessionOpened(session);
		IConnection connection = (IConnection) session.getAttribute(RPConnection.RP_CONNECTION_KEY);
		if(connection != null)
			connection.continues();
	}
	

}
