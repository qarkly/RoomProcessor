package com.iflytek.roomprocessor.net.conn;

import java.util.Collection;
import java.util.List;

public interface IRPConnManager {
	
	public IConnection getConnection(String clientId);
	
	public List<IConnection> getConnections();
	
	public IConnection createConnection(String clientId);
	
	public IConnection removeConnection(String clientId);
	
	public Collection<IConnection> removeConnections();

}
