
package com.proyecto.demo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import io.moquette.server.config.FileResourceLoader;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import javax.net.ssl.SSLContext;

import java.util.concurrent.TimeUnit;
import java.lang.InterruptedException;

import java.util.*;
import java.util.Map.Entry;

import java.net.*;
import java.io.*;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

import java.util.concurrent.ThreadPoolExecutor;

import java.util.concurrent.Semaphore;

import java.util.concurrent.TimeUnit;
import java.lang.InterruptedException;
import java.io.File;

import com.proyecto.*;

import java.util.concurrent.TimeUnit;

public class App {

	public static GestorRecursos gestorRecursos;
	public static ProtocoloInteraccionMqtt protocoloInteraccionMqtt;
	public static ProtocoloSTP protocoloSTP;
	public static InforInput inforInput;

	public static Boolean sec;

	public static Semaphore mutex = new Semaphore(1, true); // mutex para controlar el acceso a las listas de topics
															// (True=fair)
															
	public static void main(String[] args) throws InterruptedException, IOException {
		sec = false;

		if (args.length > 0) {
			if (args[0].equals("sec")) {
				sec = true;
			}

		}

		// MQTT
		File fileSetings = new File("src/main/resources/config/moquette.conf");
		IResourceLoader fylesystemLoader = new FileResourceLoader(fileSetings);
		IConfig config = new ResourceLoaderConfig(fylesystemLoader);

		// Lista de Brokers externos y datos de entrada
		inforInput = new InforInput("", "", "", 0, new ArrayList<String>());
		File ficheroServidoresExternos = new File("src/main/resources/config/fichero.txt");
		inforInput = leerDatos(ficheroServidoresExternos);

		try {
			// Creating a MQTT Broker using Moquette
			final Server mqttBroker = new Server();

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					System.out.println("Deteniendo servidor MQTT");
					mqttBroker.stopServer();
					System.out.println("Servidor detenido");
				}
			});

			// Creo el gestorRecursos despues de crear todos los servidores para evitas que sea null
			gestorRecursos = new GestorRecursos(mutex, mqttBroker, sec, inforInput);

			// Creo una instancia del protocoloSTP
			protocoloSTP = new ProtocoloSTP(gestorRecursos, mutex, sec, inforInput);
			protocoloSTP.envioPeriodicoPUBLISH();

			// Creo las instancias del protocolo de interaccion MQTT
			protocoloInteraccionMqtt = new ProtocoloInteraccionMqtt(gestorRecursos, mutex, sec, inforInput, protocoloSTP);
			
			// Arranco todos los servidores/brokers
			final List<? extends InterceptHandler> userHandlers = Arrays
					.asList(new PublisherListener(protocoloInteraccionMqtt));
			mqttBroker.startServer(config, userHandlers);

			System.out.println("Servidor corriendo, presionar ctrl-c para detenerlo..");

		} catch (IOException e) {
			e.printStackTrace();
		}

		while (true) {

		}
	}

	public static InforInput leerDatos(File fichero) {
		InforInput _inforInput = new InforInput("", "", "", 0, new ArrayList<String>());
		Scanner s = null;

		FileReader fr = null;
		BufferedReader br = null;

		String[] parts;
		int numBrokerExternos = 0;
		Boolean primeraLinea = true;

		try {
			// Apertura del fichero y creacion de BufferedReader para realizar una lectura
			s = new Scanner(fichero);

			// Leemos linea a linea el fichero
			while (s.hasNextLine()) {
				String linea = s.nextLine(); // Guardamos la linea en un String

				parts = linea.split(" ");

				if (primeraLinea) {
					_inforInput.ID = parts[0];
					_inforInput.address = parts[1];
					_inforInput.port = parts[2];
					_inforInput.C = Integer.parseInt(parts[3]); 
					System.out.println("Mi ID y mi puerto son " + _inforInput.ID + " y " + _inforInput.port + ". C: " + String.valueOf(_inforInput.C));
					primeraLinea = false;
				} else {

					if (!parts[1].equals(_inforInput.port)) {
						_inforInput.brokersMQTTExternos
								.add("Proxy-" + String.valueOf(numBrokerExternos) + " " + parts[0] + ":" + parts[1]);
						numBrokerExternos++;
					} else {
						numBrokerExternos++; // Salto mi identificador
					}
				}

			}

		} catch (Exception e) {
			System.out.println("Error de lectura de datos, excepcion: " + e.getClass().getName() + "\n" + "Mensaje: "
					+ e.getMessage());
		} finally {
			// En el finally cerramos el fichero,tanto si la lectura ha sido correcta o no
			try {
				if (s != null)
					s.close();
			} catch (Exception e2) {
				System.out.println("Mensaje 2: " + e2.getMessage());
			}
		}

		// Sacamos los borkers por pantalla
		for (int i = 0; i < _inforInput.brokersMQTTExternos.size(); i++) {
			System.out.println(_inforInput.brokersMQTTExternos.get(i));
		}
		return _inforInput;
	}
}
