package com.iflytek.roomprocessor.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.iflytek.roomprocessor.common.log.SysLogger;

public class JMXUtil {
	private static SysLogger log = new SysLogger(JMXUtil.class.getName());
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean registerNewMBean( Class clazz, Class interfaceClass) {
		boolean status = false;
		try {
			String cName = clazz.getName();
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
			}
			log.debug("Register name: "+ cName);
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.registerMBean(new StandardMBean(Class.forName(clazz.getName()).newInstance(), interfaceClass), new ObjectName("roomprocessor:type=" + cName));
			status = true;
		} catch (Exception e) {
			log.error(String.format("Could not register the %s MBean", clazz.getName()),e);
		}
		return status;
	}

}
