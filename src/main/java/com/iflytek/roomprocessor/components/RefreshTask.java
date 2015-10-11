package com.iflytek.roomprocessor.components;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.components.asynctask.AsyncTask;
import com.iflytek.roomprocessor.global.RP;
import com.iflytek.roomprocessor.global.RoomStatistics;
import com.iflytek.roomprocessor.util.JsonUtil;
import com.iflytek.roomprocessor.zookeeper.ZooKeeperWrapper;

/**
 * 
* <p>Title: RefreshTask</p>
* <p>Description:
*                1.移除内存中的空房间
*                2.删除zookeeper相应的房间节点数据
*                3.向消息队列发送移除房间id 
*                4.更新zookeeper下的roomprocesser节点数据
*                5.更新room节点下的roomprocessor节点数据</p>
* <p>Company: iflytek</p>
* @author    qarkly
* @date       2013-12-13 上午11:12:35
 */
public class RefreshTask implements Runnable{

	private final  SysLogger logger = new SysLogger(RefreshTask.class.getName());
	
	private final ScheduledExecutorService service;
	
	public RefreshTask(ScheduledExecutorService service){
	     this.service = service;	
	}
	
	private void refresh() {
		ZooKeeperWrapper zooKeeperWrapper = ZooKeeperWrapper.getInstance();
		if(zooKeeperWrapper.isStarted()){
			String[] emptyRoomsOfId = null;
			synchronized (RP.class) {
				emptyRoomsOfId = RP.removeEmptyRoom();
				if(emptyRoomsOfId != null){
					for (String roomid : emptyRoomsOfId) {
						logger.info(String.format("移除房间%s的roomprocessor子节点", roomid));
						zooKeeperWrapper.removeRoomChildRPNode(roomid);
						removeRed5(zooKeeperWrapper, roomid);
						AsyncTask.getInstance().addRemoveTask(roomid);
					}
				}
				
			}
			List<Map<String, Object>> list = RP.getRoomMessage();
			for (Map<String, Object> map : list) {
				String value = JsonUtil.serialize(map.get("RP"), Object.class);
				zooKeeperWrapper.updateRoomChildRPNode((String)map.get("roomId"), value);
			}
			RoomStatistics roomStts = RP.getAllRoomMessage();
			logger.debug("更新roomprocessor节点数据:"+roomStts.toString());
			zooKeeperWrapper.updateRoomProcessorChildNode(JsonUtil.serialize(roomStts, RoomStatistics.class));
			
		}

	}
	
	
	private  void removeRed5(ZooKeeperWrapper wrapper,String roomid){
		try {
			int size = wrapper.queryRoomChildRed5Node(roomid);
			if(size == 0){
				logger.error(String.format("房间%s的red5子节点用户数不为空，不移除red5节点", roomid));
				return;
			}
			logger.info(String.format("移除房间%s的red5子节点",roomid));
			wrapper.removeRoomChildRed5Node(roomid);
		} catch (Throwable e) {
			logger.error("查询red5节点数据出错，具体：", e);
		}
	}

	@Override
	public void run() {
		logger.info("更新zookeeper节点");
		refresh();
		service.schedule(new RefreshTask(service), Config.getInstance().getRefreshTime(), TimeUnit.SECONDS);
	}

}
