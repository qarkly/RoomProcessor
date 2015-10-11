package com.iflytek.roomprocessor.net.conn.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;

import com.iflytek.roomprocessor.api.IMessage;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.net.RPHandler;
import com.iflytek.roomprocessor.net.buffer.InBuffer;
import com.iflytek.roomprocessor.net.buffer.OutBuffer;

public class RPConnection extends BaseConnection {
	private static SysLogger logger = new SysLogger(RPConnection.class.getName());
	
	private final String clientId;
	private volatile boolean closed;
	
	
	public RPConnection(String clinetId){
		this.clientId = clinetId;
		RPHandler handler  = new RPHandler();
		handler.setConnection(this);
		this.inBuffer = new InBuffer(handler);
		this.outBuffer = new OutBuffer();
		((OutBuffer)outBuffer).setListener(this);
	}
	
	
	
	public void setIoSession(IoSession protocolSession){
		SocketAddress socketAddress = protocolSession.getRemoteAddress();
		if(socketAddress instanceof InetSocketAddress){
			remoteAddress = ((InetSocketAddress) socketAddress).getAddress().getHostAddress();
			remotePort = ((InetSocketAddress) socketAddress).getPort();
		}else {
			remoteAddress = socketAddress.toString();
			remotePort = -1;
		}
		this.session = protocolSession;
	}
	

	@Override
	public void appendIn(IMessage msg) {
		inBuffer.append(msg);
	}

	@Override
	public void flushOut() {
		((OutBuffer)outBuffer).flush();
	}

	@Override
	public void write(List<IMessage> msgs)  {
		if(session != null){
			final Semaphore lock = getLock();
			while (!closed) {
				try {
					lock.acquire();
				} catch (InterruptedException e) {
					logger.debug("等待获取写消息锁被中断"+e.getMessage());
					continue;
				}
				
				try{
					logger.debug("写消息");
					long minawritetimespan = System.currentTimeMillis();
					session.write(msgs);
					if(System.currentTimeMillis() - minawritetimespan >= 50)
						logger.warn("mina写消息耗时："+(System.currentTimeMillis() - minawritetimespan));
					break;
				}finally{
					lock.release();
				}
			}
		}

	}

	@Override
	public String getClinetId() {
		return clientId;
	}

	@Override
	public boolean isConnected() {
		return session != null && session.isConnected();
	}

	@Override
	public void close() {
		closed = true;
		if(session != null){
			session.suspendRead();
			IoFilterChain filterChains = session.getFilterChain();
			if(filterChains.contains("messageFilter")){
				filterChains.remove("messageFilter");
			}
			
			CloseFuture future = session.close(true);
			future.addListener(new IoFutureListener<CloseFuture>() {
				public void operationComplete(CloseFuture future) {
					if (future.isClosed()) {
						logger.debug("连接已经关闭！");
					} else {
						logger.debug("连接还未被关闭！");
					}
				}
			});
		}

	}

	@Override
	public String getRemoteAddress() {
		return remoteAddress;
	}

	@Override
	public int getRemotePort() {
		return remotePort;
	}

	@Override
	public boolean isInBufferEmpty() {
		return inBuffer.isEmpty();
	}

	@Override
	public boolean isOutBufferEmpty() {
		return outBuffer.isEmpty();
	}


	@Override
	public void notify(List<IMessage> list) {
		if(list == null || list.isEmpty())
			return;
		write(list);
	}



	@Override
	public void pause() {
		inBuffer.pause();
		outBuffer.pause();
	}



	@Override
	public void continues() {
		inBuffer.continues();
		outBuffer.continues();
	}



	@Override
	public void removeIoSession() {
		session = null;
	}



	@Override
	public void appendOut(IMessage msg) {
		this.outBuffer.append(msg);
	}

}
