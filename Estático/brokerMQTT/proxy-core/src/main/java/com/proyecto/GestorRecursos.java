package com.proyecto;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.*;
import java.io.*;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.Scanner;

import io.moquette.server.Server;
import io.moquette.spi.impl.subscriptions.Subscription;

import java.util.concurrent.TimeUnit;
import java.lang.InterruptedException;
import java.util.concurrent.Semaphore;

public class GestorRecursos {

	// lista de brokers distribuidos
	// public ArrayList<String> brokersMQTTExternos = new ArrayList<String>();

	public InforInput inforInput;
	public Server servidorMqtt;
	public Semaphore mutex;
	public Boolean sec;

	public GestorRecursos(Semaphore _mutex, Server _servidorMqtt, Boolean _sec, InforInput _inforInput) {
		servidorMqtt = _servidorMqtt;
		mutex = _mutex;
		sec = _sec;

		inforInput = _inforInput;
		// brokersMQTTExternos.add("Proxy-1 127.0.0.1:7778"); // lista de brokers, ID
		// que
		// usara en el client. IP:puerto del broker MQTT
		// brokersMQTTExternos.add("Proxy-2 127.0.0.1:7779");
	}

	public ArrayList getListaProxys() {
		return inforInput.brokersMQTTExternos;
	}

}
