package com.iflytek.roomprocessor;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.ThreadPool;
import com.iflytek.roomprocessor.components.rabbitmq.Subscriber;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.net.conn.IConnection;
import com.iflytek.roomprocessor.net.conn.impl.RPConnManager;
import com.iflytek.roomprocessor.zookeeper.ZooKeeperWrapper;

public class ShutDown extends Thread {

	private static final SysLogger logger = new SysLogger("ShutDown");
	@Override
	public void run() {
		logger.info("roomprocessor 正在关闭...");
		ZooKeeperWrapper wrapper = ZooKeeperWrapper.getInstance();
		wrapper.removeRoomProcessorChildNode();
		boolean isdone = true;
		while (true) {
			Collection<IConnection> connections = RPConnManager
					.getConnManager().removeConnections();
			for (IConnection iConnection : connections) {
				if (!iConnection.isInBufferEmpty()
						|| !iConnection.isOutBufferEmpty())
					isdone = false;
			}
			if (isdone)
				break;
			isdone = true;
			try {
				logger.info("业务消息未处理完,等待3秒...");
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		logger.info("关闭订阅消息...");
		for (Subscriber subscriber : RP.Subscribers) {
			subscriber.close();
		}
		shutdownAndAwaitTermination(ThreadPool.getSingleThreadExecutor());
		shutdownAndAwaitTermination(ThreadPool.getScheduledThreadExecutor());
		logger.info("系统退出...");
	}

	void shutdownAndAwaitTermination(ExecutorService pool) {
		pool.shutdown(); //关闭线程池，拒绝提交任务，已提交的任务等待完成
		try {
			//等待一段时间，让已提交的任务完成
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // 立刻取消还未完成的任务
				// 等待任务响应取消操作
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// 当前线程被中断立马取消
			pool.shutdownNow();
			// 恢复中断状态
			Thread.currentThread().interrupt();
		}
	}

}
