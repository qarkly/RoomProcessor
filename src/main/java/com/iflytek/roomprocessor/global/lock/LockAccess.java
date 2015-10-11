package com.iflytek.roomprocessor.global.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class LockAccess implements Lock{
	private static final Map<String, SoleLock> lockmap = new HashMap<String, LockAccess.SoleLock>();
	private static final Object object = new Object();
	
	private final String lockname;
	private final ReentrantLock lock;
	
	public static  Lock getLockByName(String lockname){
		synchronized(object){
			SoleLock soleLock = lockmap.get(lockname);
			if(soleLock == null){
				soleLock = new SoleLock();
				lockmap.put(lockname, soleLock);
				return new LockAccess(lockname, soleLock.lock);
			}else {
				soleLock.count.incrementAndGet();
				return new LockAccess(lockname, soleLock.lock);
			}
			
		}
		
	}
	
	private LockAccess(String lockname,ReentrantLock lock){
		this.lockname = lockname;
		this.lock = lock;
	}
	

	@Override
	public void lock() {
		lock.lock();
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		lock.lockInterruptibly();
	}

	@Override
	public boolean tryLock() {
		return lock.tryLock();
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit)
			throws InterruptedException {
		return lock.tryLock(time, unit);
	}

	@Override
	public void unlock() {
		lock.unlock();
		synchronized (object) {
			SoleLock soleLock = lockmap.get(lockname);
			if(soleLock.count.decrementAndGet() == 0){
				lockmap.remove(lockname);
			}
		}
		
	}

	@Override
	public Condition newCondition() {
		return lock.newCondition();
	}
	
    private static class SoleLock{
		public final AtomicInteger count = new AtomicInteger(1);
		
		public final ReentrantLock lock = new ReentrantLock();
	}

}
