package com.proyecto;

import java.util.ArrayList;
import java.util.List;

public class InforBPDU {

	public String ID; // Identificador del Proxy transmisor del mensaje
	public String port; // Puerto del proxy transmisor del mensaje
	public long coste_enlace; // Coste del enlace entre ambos brokers
	public long coste_hasta_el_ROOT; // Coste del broker de ID y port, hasta el ROOT
	public Broker broker;
	//public String linkStatus; // Link status. Could be: {"root", "established", "blocked"}

	public InforBPDU (String _ID, String _port, long _coste_enlace, long _coste_hasta_el_ROOT, Broker _broker) {
		ID = _ID;
		port = _port;
		coste_enlace = _coste_enlace;
		coste_hasta_el_ROOT = _coste_hasta_el_ROOT;	
		broker = _broker;
	}

}
