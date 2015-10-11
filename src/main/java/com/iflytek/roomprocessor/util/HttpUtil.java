package com.iflytek.roomprocessor.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import com.iflytek.roomprocessor.common.exception.SysException;

public class HttpUtil {

	public enum HttpMethod {
		GET, POST
	}

	private URLConnection urlc = null;
	private String encoding = "utf-8";
	private HttpMethod method = HttpMethod.GET;
	private int timeout = 2000;

	private Map<String, String> properties = new HashMap<String, String>();

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void addProperties(String key, String value) {
		if (properties.containsKey(key)) {
			properties.remove(key);
		}
		properties.put(key, value);
	}

	public HttpUtil(int timeout) {
		this.timeout = timeout;
	}

	public HttpUtil() {
	}

	public String getResponse() {
		byte[] buffer = new byte[0];

		InputStream stream;
		if (urlc != null) {
			try {

				urlc.setReadTimeout(timeout);
				stream = urlc.getInputStream();
				int count = timeout / 10;
				while (true) {
					if (count <= 0) {
						break;
					}
					if (stream.available() > 0) {
						buffer = new byte[stream.available()];
						stream.read(buffer);
						while (stream.available() > 0) {
							byte[] tmpbuffer = new byte[stream.available()];
							stream.read(tmpbuffer);
							buffer = append(buffer, tmpbuffer);
						}
						break;
					}
					Thread.sleep(10);
					count--;
				}

				String ret = new String(buffer, encoding);
				return ret;
			} catch (Throwable e) {
				throw new SysException(e, "HttpHelper获取http请求出现异常"+e.getMessage());
			}
		}
		return "";
	}

	public void sendRequest(String url, byte[] data) {
		try {
			if (method.equals(HttpMethod.GET)) {
				StringBuffer param = new StringBuffer();
				int i = 0;
				for (String key : properties.keySet()) {
					if (i == 0) {
						param.append("?");
					} else {
						param.append("&");
					}
					param.append(key).append("=").append(properties.get(key));
					i++;
				}
				url += param;
			}
			URL u = new URL(url);
			urlc = u.openConnection();
			urlc.setReadTimeout(timeout);
			urlc.addRequestProperty("method", String.valueOf(method));
			urlc.setDoOutput(true);
			urlc.setDoInput(true);
			urlc.setUseCaches(false);
			
			for (String key : properties.keySet()) {
				urlc.addRequestProperty(key, properties.get(key));
			}

			if (method.equals(HttpMethod.POST)) {
				urlc.getOutputStream().write(data);
			}

		} catch (Throwable e) {
			throw new SysException(e, "HttpHelper发送http请求出现异常"+e.getMessage());
		}

	}

	public void sendRequest(String url, String data) {
		byte[] sdata = null;
		try {
			sdata = data.getBytes(encoding);
		} catch (Throwable e) {
			throw new SysException(e, "HttpHelper发送http请求出现异常"+e.getMessage());
		}
		sendRequest(url, sdata);
	}

	public byte[] download(String url) {
		byte[] retbuffer = new byte[0];
		try {
			URL u = new URL(url);
			urlc = u.openConnection();
			urlc.setReadTimeout(timeout);
			InputStream inputStream = urlc.getInputStream();
			Thread.sleep(100);
			while (true) {
				int size = inputStream.available() > 0 ? inputStream
						.available() : 1024;
				byte[] buffer = new byte[size];
				int ret = inputStream.read(buffer);
				if (ret <= 0) {
					break;
				}
				
				if (ret == size) {
					retbuffer = append(retbuffer, buffer);
				} else {
					byte[] tmpbuffer = new byte[ret];
					System.arraycopy(buffer, 0, tmpbuffer, 0, tmpbuffer.length);
					retbuffer = append(retbuffer, tmpbuffer);
				}
			}
		} catch (Throwable e) {
			throw new SysException(e, "HttpHelper下载资源出现异常"+e.getMessage());
		}
		return retbuffer;
	}
	
	private  byte[] append(byte[] src, byte[] dst){
		if(src == null)
			return dst;
		if(dst == null)
			return src;
		byte[] byts = new byte[src.length+dst.length];
		System.arraycopy(src, 0, byts, 0, src.length);
		System.arraycopy(dst, 0, byts, src.length, dst.length);
		return byts;
	}
	



}
