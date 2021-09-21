package com.proyecto;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

//import java.util.concurrent.Semaphore;
//import java.lang.InterruptedException;

public class ClienteMQTT {

	public String ID; // Identificador del Proxy transmisor del mensaje
	public String port; // Puerto del proxy transmisor del mensaje
	public MqttClient cliente; // Conexion MQTT establecida con el Broker

	public ClienteMQTT (String _ID, String _port, MqttClient _cliente) {
		ID = _ID;
		port = _port;
		cliente = _cliente;	
	}

}
