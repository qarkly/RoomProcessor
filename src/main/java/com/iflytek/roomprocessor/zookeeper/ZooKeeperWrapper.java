package com.iflytek.roomprocessor.zookeeper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.exception.SysException;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.util.JsonUtil;


public class ZooKeeperWrapper {

	CuratorFramework client = null;

	String ip = "127.0.0.1";
	int port = 2181;
	int sessiontimeout = 30 * 1000;
	String charset = "utf-8";


	private static final String ROOMNODE = "/roomnode";
	private static final String ROOMNODECHILDROOM = "/room";
	
	private static final String ROOMPROCESSORNODE = "/roomprocessornode";
	private static final String ROOMPROCESSOR = "/roomprocessor";
	private static final String RED5 = "/red5";

	private final String RPchildnode;
	private final ConcurrentHashMap<String, String> backup = new ConcurrentHashMap<String, String>();
	SysLogger logger;

	private static class Holder{
		private final static  ZooKeeperWrapper wrapper = new ZooKeeperWrapper();
	}
	
	public static ZooKeeperWrapper getInstance(){
		return Holder.wrapper;
	}
	
	/**
	 * @param ip
	 * @param port
	 * @param sessiontimeout
	 *            session超时时间
	 */
	private ZooKeeperWrapper() {
		Config config = Config.getInstance();
		this.RPchildnode = config.getLocalhost()+":"+config.getPort();
		this.ip = config.getZooKeeperAddr();
		this.port = config.getZooKeeperPort();
		logger = new SysLogger(ZooKeeperWrapper.class.getName());

		client = CuratorFrameworkFactory.builder()
				.connectString(this.ip + ":" + String.valueOf(this.port)) // 连接字符串
				.retryPolicy(new RetryNTimes(3, 1000)) // 重连策略
				.sessionTimeoutMs(this.sessiontimeout) // session过期时间（ms）
				.build();
		
	}

//	public ZooKeeperWrapper(String ip, int port, String RPchildnode) {
//		this.RPchildnode = RPchildnode;
//		this.ip = ip;
//		this.port = port;
//		logger = new SysLogger(ZooKeeperWrapper.class.getName());
//		client = CuratorFrameworkFactory.builder()
//				.connectString(this.ip + ":" + String.valueOf(this.port)) // 连接字符串
//				.retryPolicy(new RetryNTimes(3, 1000)) // 重连策略
//				.sessionTimeoutMs(this.sessiontimeout) // session过期时间（ms）
//				.build();
//	}
	
	public boolean isStarted(){
		return client.getState().equals(CuratorFrameworkState.STARTED);
	}
	
	public void close(){
		client.close();
	}

	/**
	 * 连接
	 * 
	 * @return
	 */
	public boolean connect() {
		try {
			client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
				
				public void stateChanged(CuratorFramework framework, ConnectionState state) {
					logger.info("Zookeeper状态："+state.name());
					if(state.equals(ConnectionState.RECONNECTED)){
						logger.info("Zookeeper状态：检查节点");
						for (String key : backup.keySet()) {
							String value = backup.get(key);
							createNode(key, value);
						}
					}
				}
			});
			client.start();
			client.getZookeeperClient().blockUntilConnectedOrTimedOut();
			createRoomProcessorNode();
			return true;
		} catch (Exception e) {
			logger.error("ZooKeeperWrapper.connect出现异常", e);
			return false;
		}
	}
	/**
	 * 创建roomprocessornode永久节点
	 * @return
	 */
	
	public boolean createRoomProcessorNode(){
		logger.debug("创建roomprocessornode永久节点" );
		try {
			Stat permanentStat = client.checkExists().forPath(ROOMPROCESSORNODE);
			if(permanentStat == null){
				client.create().withMode(CreateMode.PERSISTENT)
				       .forPath(ROOMPROCESSORNODE,null);
			}
			return true;
		} catch (Exception e) {
			logger.error("创建roomprocessornode永久节点失败", e);
			return false;
		}
	}
	/**
	 * 创建roomprocessornode下的roomprocessor临时节点
	 * @param nodename
	 * @return
	 */
	public boolean createRoomProcessorChildNode(){
		return createRoomProcessorChildNode(this.RPchildnode, "");
	}
	
	public boolean createRoomProcessorChildNode(String value){
		return createRoomProcessorChildNode(this.RPchildnode, value);
	}
	/**
	 * 创建roomprocessornode下的roomprocessor
	 * 临时节点,并赋值 将节点信息添加到backup
	 * @param nodename roomprocessor的ip+prot
	 * @param value
	 * @return
	 */
	private boolean createRoomProcessorChildNode(String nodename, String value){
		logger.debug("创建roomprocessornode下的临时节点，节点名："+nodename);
		try {
			String path = ROOMPROCESSORNODE+"/"+nodename;
			backup.put(path, value);
			Stat tmpRPChildStat = client.checkExists().forPath(path);
			if(tmpRPChildStat != null)
				client.delete().forPath(path);
			client.create().withMode(CreateMode.EPHEMERAL).forPath(path, value.getBytes(charset));
			return true;
		} catch (Exception e) {
			logger.error("创建roomprocessornode下的临时节点失败",e);
			return false;
		}
	}
	
	public boolean updateRoomProcessorChildNode(String value){
		return updateRoomProcessorChildNode(this.RPchildnode, value);
	}
	

	/**
	 * 更新roomprocessornode临时子节点数据，同时更新backup中数据
	 * 
	 * @param nodename
	 * @param value
	 * @return
	 */
	private boolean updateRoomProcessorChildNode(String nodename, String value) {
		logger.debug(String.format("更新roomprocessornode子节点%s数据，值:%s",nodename,value));
		try {
			String path = ROOMPROCESSORNODE + "/" + nodename;
			backup.put(path, value);
			client.setData().forPath(path,value.getBytes(charset));
			return true;
		} catch (Exception e) {
			logger.error("ZooKeeperWrapper.updateRed5ChildNode出现异常", e);
			return false;
		}
	}
	/**
	 * 创建roomnode永久节点
	 * @return
	 */
	public boolean createRoomNode(){
		try {
			logger.debug("创建roomnode永久节点");
			Stat roomnodeStat = client.checkExists().forPath(ROOMNODE);
			if(roomnodeStat == null)
				client.create().withMode(CreateMode.PERSISTENT).forPath(ROOMNODE);
			return true;
		} catch (Exception e) {
			logger.error("创建roomnode永久节点出现异常", e);
			return false;
		}
	}

	/**
	 * 创建房间和房间信息永久节点
	 * /roomnode/room_N 和/roomnode/room_N/room
	 * @param roomname
	 * @return
	 */
	public boolean createRoom(String roomname) {
		try {
			logger.debug("创建room永久节点："+roomname);
			Stat roomStat = client.checkExists().forPath(ROOMNODE+"/"+roomname);
			if(roomStat == null)
				client.create().withMode(CreateMode.PERSISTENT).forPath(ROOMNODE+"/"+roomname);
			Stat tmpStat = client.checkExists().forPath(ROOMNODE+"/"+roomname+ROOMNODECHILDROOM);
			if(tmpStat == null)
				client.create().withMode(CreateMode.PERSISTENT).forPath(ROOMNODE+"/"+roomname+ROOMNODECHILDROOM);
			return true;
		} catch (Exception e) {
			logger.debug(String.format("创建房间%s出现异常, ", roomname)+e);
		}
		return false;
	}
	/**
	 * 更新房间的房间信息永久节点数据
	 * @param roomname
	 * @param value
	 * @return
	 */
	public boolean updateRoom(String roomname,String value){
		try {
			logger.debug(String.format("更新房间%s数据", roomname));
			client.setData().forPath(ROOMNODE+"/"+roomname+ROOMNODECHILDROOM, value.getBytes(charset));
			return true;
		} catch (Exception e) {
			logger.error(String.format("更新房间%s数据失败", roomname));
			return false;
		}
	}
	/**
	 * 创建房间的roomprocessor临时子节点
	 * @param roomname
	 * @return
	 */
	public boolean createRoomChildRPNode(String roomname){
		return createRoomChildRPNode(roomname, "");
	}
	/**
	 * 创建房间的roomprocessor临时子节点，并将节点信息添加到backup
	 * @param roomname 房间名
	 * @param value
	 * @return
	 */
	private boolean createRoomChildRPNode(String roomname,String value){
		try {
			logger.debug(String.format("创建房间%s的子节点roomprocessor", roomname));
			String path = ROOMNODE+"/"+roomname+ROOMPROCESSOR;
			backup.put(path, value);
			Stat tmpRPStat = client.checkExists().forPath(path);
			if(tmpRPStat != null)
				return false;
			client.create().withMode(CreateMode.EPHEMERAL).forPath(path,value.getBytes(charset));
			return true;
		} catch (Exception e) {
			throw new SysException(e,String.format("创建房间%s的子节点roomprocessor失败", roomname));
		}
	}
	/**
	 * 更新房间的roomprocessor临时子节点数据，并更新backup
	 * @param roomname
	 * @param value
	 * @return
	 */
	public boolean updateRoomChildRPNode(String roomname,String value){
		try {
			logger.debug(String.format("更新房间%s的子节点roomprocessor数据，值：%s", roomname,value));
			String path = ROOMNODE+"/"+roomname+ROOMPROCESSOR;
			backup.put(path, value);
			client.setData().forPath(path, value.getBytes(charset));
			return true;
		} catch (Exception e) {
			logger.error(String.format("更新房间%s的子节点roomprocessor数据失败", roomname),e);
			return false;
		}
	}
	
	public boolean removeRoomProcessorChildNode(){
		try {
			logger.info("移除roomprocessor节点:"+this.RPchildnode);
			String path = ROOMPROCESSORNODE+"/"+this.RPchildnode;
			Stat stat = client.checkExists().forPath(path);
			if(stat != null)
				client.delete().forPath(path);
			return true;
		} catch (Exception e) {
			logger.error("移除roomprocessor节点:"+this.RPchildnode+"出错", e);
			return false;
		}
	}
	
	/**
	 * 移除房间的roomprocessor临时子节点，并从backup中移除节点信息
	 * @param roomname
	 * @return
	 */
	public boolean removeRoomChildRPNode(String roomname){
		try {
			logger.debug(String.format("移除房间%s的子节点roomprocessor", roomname));
			String path = ROOMNODE+"/"+roomname+ROOMPROCESSOR;
			backup.remove(path);
			Stat tmpRPStat = client.checkExists().forPath(path);
			if(tmpRPStat != null)
				client.delete().forPath(path);
			return true;
		} catch (Exception e) {
			logger.error(String.format("移除房间%s的子节点roomprocessor失败", roomname),e);
			return false;
		}
	}
	
	/**
	 * 移除房间的red5临时子节点
	 * 
	 * @param nodename
	 * @return
	 */
	public boolean removeRoomChildRed5Node(String roomname) {
		try {
			logger.debug(String.format("移除房间%s的子节点red5", roomname));
			client.delete().forPath(ROOMNODE+"/"+roomname+RED5);
			return true;
		} catch (Exception e) {
			logger.error("ZooKeeperWrapper.deleteRed5ChildNode出现异常", e);
			return false;
		}
	}

	
	private void createNode(String nodepath,String value){
		try {
			logger.debug(String.format("重连zookeeper，回复被删除的节点%s，数据%s", nodepath,value));
			Stat nodeStat = client.checkExists().forPath(nodepath);
			if(nodeStat == null)
			   client.create().withMode(CreateMode.EPHEMERAL).forPath(nodepath, value.getBytes(charset));
		} catch (Exception e) {
			logger.error("回复被删除的节点："+nodepath+"出现异常", e);
			
		}
	}
	
	public int queryRoomChildRed5Node(String roomname) throws Throwable{
			logger.debug(String.format("查询房间%s的子节点red5数据", roomname));
			byte[] data = client.getData().forPath(
					ROOMNODE + "/" + roomname + RED5);
			String json = new String(data);
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) JsonUtil.deserialize(json, Map.class);
			int red5UserCount = (Integer) map.get("red5UserCount");
			return red5UserCount;
	}
	
/*
	public int queryRoomMaxUserCount(String roomid) {
		try {
			logger.debug("ZooKeeperWrapper.queryRoomMaxUserCount房间节点:"+ROOMNODE + "/" + roomid + ROOMNODECHILDRED);
			byte[] data = client.getData().forPath(
					ROOMNODE + "/" + roomid + ROOMNODECHILDROOM);
			String json = new String(data);
			RoomData roomdata = (RoomData) jsonUtil.deserialize(json,
					RoomData.class);
			return roomdata.maxUserCount;
		} catch (Exception e) {
			logger.error("ZooKeeperWrapper.queryRoomMaxUserCount出现异常", e);
			return -1;
		}
	}
*/
	

}
