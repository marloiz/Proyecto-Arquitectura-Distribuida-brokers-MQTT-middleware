package com.proyecto;

import java.util.ArrayList;
import java.util.List;

public class InforRTT {

	public String port; // Puerto del proxy transmisor del mensaje
	public int RTT; // Coste del broker de ID y port, hasta el ROOT


	public InforRTT (String _port, int _RTT) {
		port = _port;
		RTT = _RTT;	
	}

}
