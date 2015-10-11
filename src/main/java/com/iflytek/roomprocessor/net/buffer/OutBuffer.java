package com.iflytek.roomprocessor.net.buffer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import com.iflytek.roomprocessor.api.IMessage;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.ThreadPool;
/**
 * 
* <p>Title: OutBuffer</p>
* <p>Description:输出缓冲区，对写出消息的控制 </p>
* <p>Company: iflytek</p>
* @author    qarkly
* @date       2013-12-11 下午8:41:34
 */
public final class OutBuffer extends BaseBuffer {
	
	private static SysLogger logger =  new SysLogger(OutBuffer.class.getName());
	
	private ScheduledExecutorService excutorservice;
	
	private final ReentrantLock lock = new ReentrantLock();
		
	private IOutBufferListener listener = null;
	
	private Object object = new Object();
	private Config config = null;
	
	/**
	 * 
	 * @param delayTime 定时处理时间间隔
	 * @param maxSize  最大处理条数
	 * @param excutorservice 可安排计划任务的线程池，@newScheduledThreadPool
	 */
	public OutBuffer() {
		super();
		config  = Config.getInstance();
		this.excutorservice = ThreadPool.getScheduledThreadExecutor();
	}

	/**
	 * 
	 * @param listener 向其推送写出缓冲区内容，由IConnection实现
	 */
	public void setListener(IOutBufferListener listener){
		this.listener = listener;
	}
	
	protected boolean isrunning(){
		return isrunning;
	}
	
	protected void run(){
		synchronized (object) {
			if(!isrunning){
				isrunning = true;
				this.excutorservice.submit(new TimerTask());
				logger.debug("新消息加入输出缓冲区，激活定时处理");
			}
		}
	}
	
	protected void waitting(){
		synchronized (object) {
			if(isrunning)
				isrunning = false;
		}
	}
	
	
	@Override
	public void append(IMessage msg) {
		logger.debug(String.format("消息：%s 加入输出缓冲区", msg.toString()));
		this.queue.add(msg);
		this.run();
		if(!isPuased()){
			if(size() >= config.getMaxSize())
				this.excutorservice.submit(new NoWaitTask());
		}
		
	}
	/**
	 * 获取队列中指定大小的条数
	 * @param size
	 * @return
	 */
	private List<IMessage> acquireFixedSize(int size){
		if(size <=0 || size > queue.size())
			return null;
		List<IMessage> list = new ArrayList<IMessage>();
		queue.drainTo(list,size);
		return list;
	}
	/**
	 * 获取队列中所有可用的条数
	 * @return
	 */
	private List<IMessage> acquireAvailable(){
		List<IMessage> list = new ArrayList<IMessage>();
		queue.drainTo(list);
		return list;
	}
	/**
	 * 立即刷新缓冲区数据，写出所有数据
	 */
	public void flush(){
		if(isPuased()){
			waitting();
			return;
		}
		lock.lock();
		try{
			logger.debug("调用flush方法，立即刷新处理");
			lastUpdateTime = System.currentTimeMillis();
			listener.notify(acquireAvailable());
		}finally{
			lock.unlock();
		}
		
	}

	/**
	 * 
	* <p>Title: NoWaitTask</p>
	* <p>Description: 内部类，提交达到最大处理条数的任务</p>
	* <p>Company: iflytek</p>
	* @author    qarkly
	* @date       2013-12-11 下午8:49:28
	 */
	private class NoWaitTask implements Runnable{

		@Override
		public void run() {
			if(isPuased()){
				waitting();
				return;
			}
			lock.lock();
			try{
				logger.debug("达到最大处理条数，立即处理");
				lastUpdateTime = System.currentTimeMillis();
				List<IMessage> list = acquireFixedSize(config.getMaxSize());
				listener.notify(list);
			}finally{
				lock.unlock();
			}
			
		}
		
	}
	/**
	 * 
	* <p>Title: TimerTask</p>
	* <p>Description:内部类，提交定时处理的任务 </p>
	* <p>Company: iflytek</p>
	* @author    qarkly
	* @date       2013-12-11 下午8:49:55
	 */
	private class TimerTask implements Runnable{

		@Override
		public void run() {
			if(isPuased()){
				waitting();
				return;
			}
			if(isrunning()){
				lock.lock();
				try{
					if(size() > 0){
						long delay = System.currentTimeMillis() - lastUpdateTime;
						if(delay >= config.getDelayTime()){
							logger.debug("定时处理输出缓冲区内容");
							lastUpdateTime = System.currentTimeMillis();
							listener.notify(acquireAvailable());
							excutorservice.schedule(new TimerTask(), config.getDelayTime(), TimeUnit.MILLISECONDS);
						}else {
							excutorservice.schedule(new TimerTask(), config.getDelayTime()-delay, TimeUnit.MILLISECONDS);
						}
						
					}else {
						waitting();
						logger.debug("输出缓冲区内容为空，暂停处理");
					}
					
				}finally{
					lock.unlock();
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
