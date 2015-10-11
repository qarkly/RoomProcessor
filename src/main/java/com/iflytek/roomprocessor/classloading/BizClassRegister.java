package com.iflytek.roomprocessor.classloading;

import java.util.HashMap;
import java.util.Map;

import com.iflytek.roomprocessor.jmx.mxbeans.BizRegisterMXBean;

public class BizClassRegister implements BizRegisterMXBean {

	public static final Map<Integer, String> bizs = new HashMap<Integer, String>();
	@Override
	public Map<Integer, String> getRegisterBiz() {
		return bizs;
	}

	@Override
	public boolean modifyBiz(int bizType, String name,String path) {
		String classname  = bizs.get(bizType);
		if(classname == null)
			return false;
		bizs.put(bizType, name);
		return ClassReloadHelper.getInstance().reloadBizClass(bizType, name,path);
	}

	@Override
	public boolean addBiz(int bizType, String name,String path) {
		String classname = bizs.get(bizType);
		if(classname != null)
			return false;
		bizs.put(bizType, name);
		return ClassReloadHelper.getInstance().reloadBizClass(bizType, name,path);
	}

}
