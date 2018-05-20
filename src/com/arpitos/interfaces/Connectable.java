package com.arpitos.interfaces;

/**
 * Used for any class which implements connections, If implemented then same
 * class can be used with common HeartBeat mechanism
 * 
 * @author ArpitS
 *
 */
public interface Connectable {

	public void connect() throws Exception;

	public void disconnect() throws Exception;

	public boolean isConnected();

	public void sendMsg(byte[] data) throws Exception;

	public byte[] getNextMsg() throws Exception;

	public boolean hasNextMsg();

}
