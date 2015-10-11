package com.iflytek.roomprocessor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.iflytek.roomprocessor.classloading.BizClassRegister;
import com.iflytek.roomprocessor.classloading.ClassReloadHelper;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.config.ConfigModify;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.RefreshTask;
import com.iflytek.roomprocessor.components.ThreadPool;
import com.iflytek.roomprocessor.components.rabbitmq.Subscriber;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.jmx.JMXUtil;
import com.iflytek.roomprocessor.jmx.mxbeans.BizRegisterMXBean;
import com.iflytek.roomprocessor.jmx.mxbeans.ConfigModifyMXBean;
import com.iflytek.roomprocessor.net.Server;
import com.iflytek.roomprocessor.zookeeper.ZooKeeperWrapper;

public class BootStrap {

	private static final SysLogger logger = new SysLogger("BootStrap");

	public static void main(String[] args) {
		logger.info("启动roomprocessor进程...");
		Config.getInstance().setIndex(Integer.valueOf(args[0])-1);
		registerHook();
		registerJMXBean();
		registerRPNode();
		startSubscribe();
		startRefreshTask();
		startServer();

	}
	
	public static void registerJMXBean(){
		logger.info("加载业务类，注册JMXBean...");
		ClassReloadHelper.getInstance();
		JMXUtil.registerNewMBean(BizClassRegister.class, BizRegisterMXBean.class);
		JMXUtil.registerNewMBean(ConfigModify.class, ConfigModifyMXBean.class);
	}

	public static void startSubscribe() {
		logger.info("启动订阅消息");
		Config config = Config.getInstance();
		Subscriber subscriber = new Subscriber(config.getRPChannel());
		subscriber.start();
		RP.Subscribers.add(subscriber);
		subscriber = new Subscriber("ROOM_PROCESSOR_" + config.getLocalhost() + ":"
				+ config.getPort());
		subscriber.start();
		RP.Subscribers.add(subscriber);
	}

	public static void startRefreshTask() {
		logger.info("启动zookeeper定时更新线程");
		Config config = Config.getInstance();
		ScheduledExecutorService service = ThreadPool
				.getScheduledThreadExecutor();
		service.schedule(new RefreshTask(service), config.getRefreshTime(), TimeUnit.SECONDS);
	}

	public static void registerHook() {
		logger.info("注册进程关闭钩子...");
		Runtime.getRuntime().addShutdownHook(new ShutDown());
	}

	public static void registerRPNode() {
		logger.info("连接Zookeeper,创建节点");
		ZooKeeperWrapper zooKeeper = ZooKeeperWrapper.getInstance();
		zooKeeper.connect();
		while (!zooKeeper.isStarted()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		zooKeeper.createRoomNode();
		zooKeeper.createRoomProcessorChildNode();
	}

	public static void startServer() {
		logger.info("启动socket server");
		Server server = new Server();
		server.serverStart();
	}
}
