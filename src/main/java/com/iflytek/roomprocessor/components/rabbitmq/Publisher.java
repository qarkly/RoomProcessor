package com.iflytek.roomprocessor.components.rabbitmq;

import java.net.URLEncoder;
import java.util.Map;

import com.iflytek.roomprocessor.api.IMessage;
import com.iflytek.roomprocessor.common.config.Config;
import com.iflytek.roomprocessor.common.log.SysLogger;
import com.iflytek.roomprocessor.util.HttpUtil;
import com.iflytek.roomprocessor.util.JsonUtil;
import com.iflytek.roomprocessor.util.HttpUtil.HttpMethod;

public class Publisher {
	private static final SysLogger logger = new SysLogger(
			Publisher.class.getName());
	private static final String charset = "utf-8";
	private static final String Success = "0000";

	@SuppressWarnings("unchecked")
	public static void publish(IMessage msg) {
		Config config = Config.getInstance();
		try {
			String data = JsonUtil.serialize(msg, Object.class);
			HttpUtil http = new HttpUtil(config.getHttpTimeOut());
			http.setMethod(HttpMethod.POST);
			http.addProperties("Accept", "application/json");
			data = URLEncoder.encode(data, charset);
			String channel = URLEncoder.encode(config.getRPChannel(), charset);
			http.sendRequest(config.getOperateInterface() + "/notify/publish/",
					String.format("channel=%s&&notify=%s", channel, data));
			String result = http.getResponse();
			Object object = JsonUtil.deserialize(result, Object.class);
			if (object == null)
				logger.error("解析publish结果出错");
			else {
				Map<String, Object> map = (Map<String, Object>) object;
				String returncode = (String) map.get("returnCode");
				if (Success.equals(returncode))
					logger.info("publish消息成功,消息内容:" + data);
				else {
					logger.error(String.format(
							"publish消息失败,调用接口返回结果:%s publish消息内容:%s", result,
							data));

				}
			}
		} catch (Exception e) {
			logger.error("publish消息出错", e);
		}
	}
}
