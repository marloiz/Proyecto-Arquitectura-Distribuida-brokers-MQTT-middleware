package com.proyecto;

import java.util.ArrayList;
import java.util.List;

public class InforInput {

	public String ID; // Identificador del Broker transmisor del mensaje
	public String port; // Puerto del Broker transmisor del mensaje
	public String address;
	public int C;
	public ArrayList<String> brokersMQTTExternos = new ArrayList<String>(); //Lista de brokers externos
	


	public InforInput(String _ID, String _port, String _address, int _C, ArrayList<String> _brokersMQTTExternos) {
		ID = _ID;
		port = _port;
		address = _address;
		C = _C;
		brokersMQTTExternos = _brokersMQTTExternos;
	}


}

