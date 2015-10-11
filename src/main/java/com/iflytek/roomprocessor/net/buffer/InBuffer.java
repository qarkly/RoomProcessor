package com.iflytek.roomprocessor.net.buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.iflytek.roomprocessor.api.IHandler;
import com.iflytek.roomprocessor.api.IMessage;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.ThreadPool;
/**
 * 
* <p>Title: InBuffer</p>
* <p>Description:输入缓冲区，用于缓存客户端发送数据和处理节奏控制 </p>
* <p>Company: iflytek</p>
* @author    qarkly
* @date       2013-12-11 下午8:29:38
 */
public final class InBuffer extends BaseBuffer {

	private static SysLogger logger = new SysLogger(InBuffer.class.getName());
	
	private IHandler handler;
	
	private ScheduledExecutorService excutorservice;
	
	private final ReentrantLock lock = new ReentrantLock();
	
	private Object object = new Object();
	
	private Config config = null;
	
	/**
	 * 
	 * @param delayTime 定时处理时间间隔
	 * @param maxSize   最大处理条数
	 * @param handler   消息处理器
	 * @param excutorservice  用来提交消息处理任务的线程池，@newScheduledThreadPool
	 */
	public InBuffer( IHandler handler) {
		super();
		config  = Config.getInstance();
		this.handler = handler;
		this.excutorservice = ThreadPool.getScheduledThreadExecutor();
		
	}
	public InBuffer( ) {
		super();
		config  = Config.getInstance();
		this.excutorservice = ThreadPool.getScheduledThreadExecutor();
		
	}
	
	public void setHandler(IHandler handler){
		this.handler = handler;
	}
	
	protected void run(){
		synchronized (object) {
			if(!isrunning){
				isrunning = true;
				this.excutorservice.schedule(new TimerTask()
				, 0, TimeUnit.MILLISECONDS);
				logger.debug("新消息加入输入缓冲区，激活定时处理");
			}
		}
	}
	
	protected void waitting(){
		synchronized (object) {
			if(isrunning)
				isrunning = false;
		}
	}
	

	/**
	 * 将消息加入缓冲区，等待处理
	 */
	@Override
	public void append(IMessage msg) {
		logger.debug(String.format("消息：%s 加入输入缓冲区", msg.toString()));
		this.queue.add(msg);
		this.run();
			
	}
	
	private void handle(){
		lock.lock();
		try {
			lastUpdateTime = System.currentTimeMillis();
			if(handler != null)
			    handler.handle(acquireAvailable());
		} finally{
			lock.unlock();
		}
	}
	/**
	 * 获取队列中所有可用的消息
	 * @return
	 */
	private List<IMessage> acquireAvailable(){
		List<IMessage> list = new ArrayList<IMessage>();
		this.queue.drainTo(list,config.getMaxSize());
		return list;
	}

	/**
	 * 
	* <p>Title: NoWaitTask</p>
	* <p>Description: 内部类，用来提交达到最大处理条数任务</p>
	* <p>Company: iflytek</p>
	* @author    qarkly
	* @date       2013-12-11 下午8:32:18
	 */
//	private class NoWaitTask implements Runnable{
//       @Override
//		public void run() {
//    	   logger.debug("达到最大处理条数，立即处理输入缓冲区内容");
//			handle();
//		}
//		
//	}
	/**
	 * 
	* <p>Title: TimerTask</p>
	* <p>Description:内部类，用来提交定时任务 </p>
	* <p>Company: iflytek</p>
	* @author    qarkly
	* @date       2013-12-11 下午8:33:37
	 */
	private class TimerTask implements Runnable{
		@Override
		public void run() {
			if(isPuased()){
				logger.debug("输入缓冲区被暂定！");
				waitting();
				return;
			}
			if(isrunning()){
				long delay = System.currentTimeMillis() - lastUpdateTime;
				if(size() > 0){
					if( delay >= config.getDelayTime()){
						logger.debug("定时处理输入缓冲区内容");
						handle();
						excutorservice.schedule(new TimerTask(), config.getDelayTime(), TimeUnit.MILLISECONDS);
					}else {
						excutorservice.schedule(new TimerTask(), config.getDelayTime()-delay, TimeUnit.MILLISECONDS);
					}
				}else {
					waitting();
					logger.debug("输入缓冲区为空，暂停处理");
				}
				
			}
		}
		
	}

	@Override
	public void pause() {
		paused = true;
		
	}
	@Override
	public void continues() {
		paused = false;
		run();
	}

}
