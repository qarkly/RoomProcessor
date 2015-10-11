package com.iflytek.roomprocessor.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.iflytek.roomprocessor.common.log.SysLogger;


public class Config {
	private final static SysLogger logger = new SysLogger(Config.class.getName());
	private  Properties properties = null;
	private  static Config config =  new Config();
	private int index = 0;
	
	private Config(){
		InputStream stream = null;
		try {
			properties = new Properties();
			ClassLoader classLoader = this.getClass().getClassLoader();
			stream = classLoader.getResourceAsStream("config/config.properties");
			properties.load(stream);
			
		} catch (Exception e) {
			logger.error("加载配置资源文件错误:", e);
		}finally{
			if(stream !=null)
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	public static Config getInstance(){
		return config;
	}
	
	public Properties getProperties(){
		return properties;
	}
	
	public void setIndex(int index){
		this.index = index;
	}
	
	public String getLocalhost(){
		String localhost = "";
		localhost = properties.getProperty("localhost");
		String[] hosts = localhost.split(",");
		return hosts[index];
	}

	public int getPort(){
		String value = properties.getProperty("port");
		String[] ports = value.split(",");
		return Integer.valueOf(ports[index]);
	}
	
	public String getZooKeeperAddr(){
		String addr = properties.getProperty("zookeeperip");
		return addr == null ? "" :addr;
	}
	
	public int getZooKeeperPort(){
		String value = properties.getProperty("zookeeperport");
		return value == null ? 0 :Integer.valueOf(value);
	}
	
	public long getDelayTime(){
		String delaytime = properties.getProperty("delaytime");
		return delaytime == null ? 0 :Long.valueOf(delaytime);
		
	}
	
	public int getMaxSize(){
		String maxsize = properties.getProperty("maxsize");
		return maxsize==null ? 0 : Integer.valueOf(maxsize);
	}
	
	public long getRoomTimeOut(){
		String timeout = properties.getProperty("roomtimeout");
		return timeout==null ? 0L : Long.valueOf(timeout);
	}
	
	public long getRefreshTime(){
		String refreshtime = properties.getProperty("refreshtimer");
		return refreshtime==null ? 0L : Long.valueOf(refreshtime);
	}
	
	public String getQueryRoominfoUrl(){
		String httpurl = properties.getProperty("queryroominfourl");
		return httpurl==null?"":httpurl;
	}
	
	public String getRabbitMqUrl(){
		String rabbitmqhost = properties.getProperty("rabbitmqhost");
		return rabbitmqhost == null ? "":rabbitmqhost;
	}
	
	public int getRabbitMqPort(){
		String rabbitmqPort = properties.getProperty("rabbitmqport");
		return rabbitmqPort == null ? 0:Integer.valueOf(rabbitmqPort);
	}
	
	public String getOperateInterface(){
		String operateinterface = properties.getProperty("operateinterface");
		return operateinterface == null ? "" : operateinterface;
	}
	
	public int getHttpTimeOut(){
		String timeout = properties.getProperty("httptimeout");
		return timeout == null ? 3000:Integer.valueOf(timeout);
	}
	
	public String getRPChannel(){
		String channel = properties.getProperty("allroomprocessorchannel");
		return channel;
	}
	
	public String getAsyncTaskQueueName(){
		String queuename = properties.getProperty("asynctaskqueuename");
		return queuename;
	}
	
	public String getRouterUrl(){
		String url = properties.getProperty("routerurl");
		return url;
	}
	
	public String[] getClassnames(){
		String classnames = properties.getProperty("bizclassname");
		return classnames.split(",");
	}
}
