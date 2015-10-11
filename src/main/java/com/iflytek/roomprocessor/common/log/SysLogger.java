package com.iflytek.roomprocessor.common.log;

import org.apache.log4j.Logger;


public class SysLogger {
	private Logger logger = Logger.getLogger(SysLogger.class);

	private String sessionID = "";

	public SysLogger(String sessionID) {
		this.sessionID = sessionID;
	}
	
	public boolean isDebugEnabled(){
		return logger.isDebugEnabled();
	}
	

	public void debug(String message) {
		if (!loggerCheck()) {
			return;
		}
		logger.debug("【" + this.sessionID + "】" + message);
	}

	public void info(String message) {
		if (!loggerCheck()) {
			return;
		}
		logger.info("【" + this.sessionID + "】" + message);
	}

	public void warn(String message) {
		if (!loggerCheck()) {
			return;
		}
		logger.warn("【" + this.sessionID + "】" + message);
	}

	public void error(String message) {
		if (!loggerCheck()) {
			return;
		}
		logger.error("【" + this.sessionID + "】" + message);
	}
	
	public void error(String message,Throwable e){
		logger.error("【" + this.sessionID + "】" + message, e);
	}

//	public void error(String message, Throwable e) {
//		if (!loggerCheck()) {
//			return;
//		}
//
//		if (e == null) {
//			logger.error("【" + this.sessionID + "】" + message);
//		} else {
//			StringBuilder sb = new StringBuilder();
//			StackTraceElement[] estack = e.getStackTrace();
//			for (StackTraceElement stackTraceElement : estack) {
//				sb.append(stackTraceElement.toString());
//			}
//			logger.error("【" + this.sessionID + "】" + message + e.getMessage()
//					+ sb.toString());
//		}
//	}

	private boolean loggerCheck() {
		// 判断logger是否为空，如果为空则重新获取
		if (logger == null) {
			try {
				logger = Logger.getLogger(SysLogger.class);
			} catch (Throwable e) {

			}
			// 如果获取后logger仍然为空，退出
			if (logger == null) {
				return false;
			}
		}
		return true;
	}
}
