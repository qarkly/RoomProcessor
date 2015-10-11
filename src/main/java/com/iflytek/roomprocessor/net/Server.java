package com.iflytek.roomprocessor.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.log.SysLogger;

public class Server {
	private final SysLogger logger  = new SysLogger(Server.class.getName());
	public void serverStart(){
		Config config = Config.getInstance();
		try {
			IoAcceptor acceptor = new NioSocketAcceptor();
			acceptor.setHandler(new RPMinaIoHandler());
			acceptor.bind(new InetSocketAddress(config.getPort()));
		} catch (IOException e) {
			logger.error("组件启动失败：", e);
		}
	}

}
