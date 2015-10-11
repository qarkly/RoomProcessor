package com.iflytek.roomprocessor.util;

import org.codehaus.jackson.map.ObjectMapper;

import com.iflytek.roomprocessor.common.exception.SysException;

public class JsonUtil {

	@SuppressWarnings("rawtypes")
	public static String serialize(Object obj, Class clazz) {
		String serialValue = "";
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerSubtypes(clazz);
			serialValue = mapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new SysException(e, "JsonUtil序列化出错");
		}
		return serialValue;
	}

	public static <T> Object deserialize(String json, Class<T> clazz) {
		T o = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerSubtypes(clazz);
			o = mapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new SysException(e, "JsonUtil反序列化出错");
		}
		return o;
	}
	
	
}
