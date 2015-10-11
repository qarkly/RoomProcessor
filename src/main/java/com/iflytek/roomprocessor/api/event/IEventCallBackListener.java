package com.iflytek.roomprocessor.api.event;

public interface IEventCallBackListener {

	/**
	 * 基于事件机制的回调，业务类处理完成用户进入房间回调通知<code>RPHandler</code>类
	 * @param isSuccess
	 *         用户进入房间是否成功成功
	 * @param hashId
	 *         用户hashId
	 * @param sId
	 *         用户sId
	 * @param roomId
	 *         进入房间的roomId
	 * @see
	 *     <code>RPHandler</code>中实现
	 */
    public void notifyRoomEntered(boolean isSuccess,String hashId,String sId,String roomId);
    /**
	 * 基于事件机制的回调，业务类处理完成用户进入房间回调通知<code>RPHandler</code>类
	 * @param isSuccess
	 *         用户进入房间是否成功成功
	 * @param hashId
	 *         用户hashId
	 * @param sId
	 *         用户sId
	 * @param roomId
	 *         进入房间的roomId
	 * @see
	 *     <code>RPHandler</code>中实现
	 */
	public void notifyRoomLeft(boolean isSuccess,String hashId,String sId,String roomId);
}
