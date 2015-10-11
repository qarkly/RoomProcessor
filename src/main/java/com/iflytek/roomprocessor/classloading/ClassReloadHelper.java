package com.iflytek.roomprocessor.classloading;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.iflytek.roomprocessor.api.event.IEventListener;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.log.SysLogger;

public class ClassReloadHelper {
	private final SysLogger logger = new SysLogger("ClassReloadHelper");

	private final Map<Integer, IEventListener> map = new ConcurrentHashMap<Integer, IEventListener>();
	
	static class Holder{
		private static final ClassReloadHelper helper = new ClassReloadHelper();
	}
	
	public static ClassReloadHelper getInstance(){
		return Holder.helper;
	}
	
	private ClassReloadHelper(){
		loadBizClass();
	}
	
	public void loadBizClass(){
		Config config = Config.getInstance();
		String[] classes = config.getClassnames();
		URL[] urls = new URL[classes.length];
		int index = 0;
		for (String classname : classes) {
			String[] key_value = classname.split(":");
			int key = Integer.valueOf(key_value[0]);
			BizClassRegister.bizs.put(key, key_value[1]);
			String path = key_value[1].replace('.', '/').concat(".class");
			URL url = ClassReloadHelper.class.getClassLoader().getResource(path);
			urls[index] = url;
			index++;
		}
		ChildFirstClassLoader classLoader = new ChildFirstClassLoader(urls);
		for(Integer key:BizClassRegister.bizs.keySet()){
			String classname = BizClassRegister.bizs.get(key);
			try {
				IEventListener listener = (IEventListener) classLoader.loadClass(classname).newInstance();
				map.put(key, listener);
			} catch (Throwable e) {
				logger.error("初始业务类出错", e);
			} 
		}
	}
	
	public boolean reloadBizClass(int bizType,String classname,String path){
		if(classname == null)
			return false;
		File file = new File(path);
		if(!(file.exists() && file.isFile()))
			return false;
		try {
			URL url = file.toURI().toURL();
			ChildFirstClassLoader classLoader = new ChildFirstClassLoader(new URL[]{url});
			IEventListener listener = (IEventListener) classLoader.loadClass(classname,url).newInstance();
			listener.roomEnterHandle(null, null, null);
			map.put(bizType, listener);
			return true;
		} catch (Throwable e) {
			logger.error("重载业务类失败", e);
			return false;
		}
	}
	
	public IEventListener getBizClass(int bizType){
		return map.get(bizType);
	}
	
	public List<IEventListener> getAllBizClasses(){
		Map<Integer,IEventListener> copy = new HashMap<Integer,IEventListener>();
		copy.putAll(map);
		List<IEventListener> list = new ArrayList<IEventListener>(copy.values());
		return list;
	}
}
