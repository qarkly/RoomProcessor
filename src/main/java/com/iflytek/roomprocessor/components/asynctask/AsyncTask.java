package com.iflytek.roomprocessor.components.asynctask;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.exception.SysException;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.ThreadPool;
import com.iflytek.roomprocessor.global.Room;
import com.iflytek.roomprocessor.util.HttpUtil;
import com.iflytek.roomprocessor.util.HttpUtil.HttpMethod;
import com.iflytek.roomprocessor.util.JsonUtil;

public class AsyncTask {
	private SysLogger sysLogger = new SysLogger(AsyncTask.class.getName());
	private String charset = "utf-8";
	private static final String Success = "0000";
	
	private static class Hodler{
		private static AsyncTask task = new AsyncTask();
	}
	
	public static AsyncTask getInstance(){
		return Hodler.task;
	}
	
	private AsyncTask(){
		super();
	}

	public void addTask(Object object) {
		try {
			Config config = Config.getInstance();
			HttpUtil http = new HttpUtil(config.getHttpTimeOut());
			http.setMethod(HttpMethod.POST);
			http.addProperties("Accept", "application/json");
			String queueName = URLEncoder.encode(
					config.getAsyncTaskQueueName(), charset);
			String msg = JsonUtil.serialize(object, Object.class);
			msg = URLEncoder.encode(msg, charset);
			http.sendRequest(config.getOperateInterface() + "/task/add/",
					String.format("queueName=%s&message=%s", queueName, msg));
			String response = http.getResponse();
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) JsonUtil
					.deserialize(response, Map.class);
			String returncode = (String) map.get("returnCode");
			if (Success.equals(returncode))
				sysLogger.info("添加异步任务成功");
			else {
				throw new SysException(response);
			}
		} catch (Throwable e) {
			sysLogger.error("发送异步任务出错:", e);
		}
	}
	
	 /**
	    * 用户离开房间后添加用户离开房间异步任务
	    * @param hashId
	    * @param sId
	    * @param roomId
	    */
		public void removeUserAsyncTask(String hashId,String sId,String roomId){
			final Map<String, Object> map  = new LinkedHashMap<String, Object>();
			map.put("t", "userLeaveRoom");
			Map<String, Object> dMap = new LinkedHashMap<String, Object>();
			Map<String, Object> uMap = new LinkedHashMap<String, Object>();
			uMap.put("sid", sId);
			uMap.put("hid", hashId);
			Map<String, Object> rMap = new LinkedHashMap<String, Object>();
			rMap.put("rid", Integer.valueOf(roomId));
			dMap.put("u", uMap);
			dMap.put("r", rMap);
			map.put("d", dMap);
			ThreadPool.getSingleThreadExecutor().submit(new Runnable() {
				
				@Override
				public void run() {
					addTask(map);
				}
			});
			
		}
		/**
		 * 用户成功进入房间添加用户进入房间异步任务
		 * @param sId
		 * @param hashId
		 * @param roomId
		 */
		public void addUserAsyncTask(String sId,String hashId,String roomId){
			final Map<String, Object> map  = new LinkedHashMap<String, Object>();
			map.put("t", "userInRoom");
			Map<String, Object> dMap = new LinkedHashMap<String, Object>();
			Map<String, Object> uMap = new LinkedHashMap<String, Object>();
			uMap.put("sid", sId);
			uMap.put("hid", hashId);
			Map<String, Object> rMap = new LinkedHashMap<String, Object>();
			rMap.put("rid", Integer.valueOf(roomId));
			dMap.put("u", uMap);
			dMap.put("r", rMap);
			map.put("d", dMap);
			ThreadPool.getSingleThreadExecutor().submit(new Runnable() {
				
				@Override
				public void run() {
					addTask(map);
				}
			});
		}
		
		public void addNewRoomAsyncTask(final Room room){
			Runnable run = new Runnable() {
				
				@Override
				public void run() {
					
					Map<String, Object> map = constructRoomInfoMap(room);
					addTask(map);
					
				}
				
				public Map<String, Object> constructRoomInfoMap(Room room){
					Map<String, Object> map = new HashMap<String, Object>();
					Map<String, Object> d = new HashMap<String, Object>();
					Map<String, Object> roominfo = new HashMap<String, Object>();
					roominfo.put("rid", room.getRoomId());
					roominfo.put("rno", "");
					roominfo.put("biz", room.getBizType());
					roominfo.put("rn", room.getRoomName());
					Integer[] chs = (Integer[]) room.getChannelNos().toArray();
					if(chs != null ){
						String[] channels = new String[chs.length];
						for (int i = 0; i < chs.length; i++) {
							channels[i] = String.valueOf(chs[i]);
						}
						roominfo.put("chs", channels);
					}else {
						roominfo.put("chs", null);
					}
					roominfo.put("maxUser", room.getMaxUserCount());
					Config config = Config.getInstance();
					roominfo.put("rpc", config.getLocalhost()+":"+config.getPort());
					roominfo.put("red", room.getRed5Id());
					d.put("r", roominfo);
					map.put("t", "newRoom");
					map.put("d", d);
					return map;
				}
			};
			ThreadPool.getSingleThreadExecutor().submit(run);
			
		}
		
		public  void addRemoveTask(final String roomid){
			Runnable run = new Runnable() {
				
				@Override
				public void run() {
					Map<String, Object> room = new HashMap<String, Object>();
					Map<String, Object> d = new HashMap<String, Object>();
					Map<String, Object> r = new HashMap<String, Object>();
					r.put("rid", Integer.valueOf(roomid));
					d.put("r", r);
					room.put("d", d);
					room.put("t", "removeRoom");
					addTask(room);
					
				}
			};
			ThreadPool.getSingleThreadExecutor().submit(run);
		}
}
