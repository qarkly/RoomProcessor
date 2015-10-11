package com.iflytek.roomprocessor.net.filter;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.net.message.Notify;
import com.iflytek.roomprocessor.util.JsonUtil;

public class NotifyFilter extends  IoFilterAdapter{
	private final static SysLogger logger = new SysLogger(NotifyFilter.class.getName());

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session,
			Object message) throws Exception {
		if(message instanceof String){
			logger.debug("收到消息，内容："+(String)message);
			try {
				Notify[] notifies = (Notify[]) JsonUtil.deserialize((String)message, Notify[].class);
				for (Notify notify : notifies) {
					nextFilter.messageReceived(session, notify);
				}
				
			} catch (Throwable e) {
				logger.error("json反序列化失败：", e);
			}
		    return;
		}
	}

	@Override
	public void filterWrite(NextFilter nextFilter, IoSession session,
			WriteRequest writeRequest) throws Exception {
		try {
			Object object = writeRequest.getMessage();
			String message = JsonUtil.serialize(object, Object.class);
		    WriteRequest request = new DefaultWriteRequest(message, writeRequest.getFuture(), writeRequest.getDestination());
		    nextFilter.filterWrite(session, request);
		    return;
		} catch (Throwable e) {
			logger.error("将消息转换成json格式出错,具体:", e);
		}
		
	}


	

}
