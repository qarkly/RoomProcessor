package com.iflytek.roomprocessor.components;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ThreadPool {

	private static class ThreadPoolHolder{
		private static final ExecutorService executorservice = Executors.newSingleThreadExecutor();
		private static final ScheduledExecutorService scheduledexecutorservice = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()+1);
	}
	
	public static ExecutorService getSingleThreadExecutor(){
		return ThreadPoolHolder.executorservice;
	}
	
	public static ScheduledExecutorService getScheduledThreadExecutor(){
		return ThreadPoolHolder.scheduledexecutorservice;
	}
}
