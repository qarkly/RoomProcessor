package com.iflytek.roomprocessor.jmx.mxbeans;

import java.util.Map;

import javax.management.MXBean;

@MXBean
public interface BizRegisterMXBean {

	
	public Map<Integer, String> getRegisterBiz();
	
	public boolean modifyBiz(int bizType,String name,String path);
	
	public boolean addBiz(int bizType,String name,String path);
}
