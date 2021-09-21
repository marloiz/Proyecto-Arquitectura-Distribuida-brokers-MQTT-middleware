package com.proyecto;

import java.util.ArrayList;
import java.util.List;

public class Broker {

	public String my_ID; // Identificador del Broker
	public String my_port; // Puerto del Broker
	public String my_address; // Direccion del Broker
	public int my_C; // Capacidad del Broker

	public String root_ID; // Identificador del Broker ROOT
	public String root_port; // Puerto del Broker ROOT
	public String root_address; // Direccion del Broker ROOT
	public int root_C; // Capacidad del Broker ROOT

	public long RTT;

	// public int P; // Link cost
	// public String linkStatus; // Link status. Could be: {"root", "established",
	// "blocked"}

	public Broker(String _my_ID, String _my_port, String _my_address, int _my_C, String _root_ID, String _root_port,
			String _root_address, int _root_C, long _RTT) {
		my_ID = _my_ID;
		my_port = _my_port;
		my_address = _my_address;
		my_C = _my_C;
		root_ID = _root_ID;
		root_port = _root_port;
		root_address = _root_address;
		root_C = _root_C;
		RTT = _RTT;
	}

	/*
	 * public void setC(int _C) { C = _C; }
	 * 
	 * public int getC() { return C; }
	 * 
	 * public void setP(int _P) { P = _P; }
	 * 
	 * public int getP() { return P; }
	 * 
	 * public void setLinkStatus(String _linkStatus) { linkStatus = _linkStatus; }
	 * 
	 * public String getLinkStatus() { return linkStatus; }
	 */

}