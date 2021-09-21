package com.proyecto;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import java.text.*;
import java.util.Date;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Semaphore;
import java.lang.InterruptedException;

import java.util.Calendar;
import java.util.TimeZone;

import java.lang.Thread;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.event.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.SECONDS;


public class ProtocoloSTP {

	public static int ALFA = 2;
	public static int BETA = 3;

	private GestorRecursos gestorRecursos;
	private Semaphore mutex;
	private Boolean sec;
	private InforInput inforInput;

	public ArrayList<ClienteMQTT> conexiones_STP = new ArrayList<ClienteMQTT>(); // Lista con todas las conexiones para transmitir los BPDUs
																					// ID + puerto + Conexion

	public ArrayList<String> puertos_BLOQUEADOS = new ArrayList<String>(); // Lista de puertos bloqueados
	public ArrayList<String> puertos_DESIGNADOS = new ArrayList<String>(); // Lista de puertos designados

	public Broker miBroker = new Broker("", "", "", 0, "", "", "", 0, 0);
	public Broker brokerROOT = new Broker("", "", "", 0, "", "", "", 0, 0);

	public ArrayList<InforBPDU> inforBPDU = new ArrayList<InforBPDU>(); // Lista que guarda la informacion recibida en los BPDU ligada a TimeOuts
	public ArrayList<InforBPDU> inforBPDUTotal = new ArrayList<InforBPDU>(); // Lista que guarda la informacion recibida en los BPDU

	public ArrayList <InforTimeOut> listaTimeOuts = new ArrayList <InforTimeOut>();

	Timer timerGeneracionPUB;
	Timer timerMuestraLista;
	boolean ha_caido_un_puerto = false;
	String puerto_que_ha_fallado = "";


	public ProtocoloSTP(GestorRecursos _gestorRecursos, Semaphore _mutex, Boolean _sec, InforInput _inforInput) {
		gestorRecursos = _gestorRecursos;
		mutex = _mutex;
		sec = _sec;
		inforInput = _inforInput;
	}

	public void envioPeriodicoPUBLISH() {
		inicioProtocolo(); 


		Timer timerGeneracionConexiones = new Timer(30000, new ActionListener() // Un  minuto
		{
			public void actionPerformed(ActionEvent e) {

					System.out.println("Creando la conexion con los clientes");
					System.out.println("");
					creacion_conexiones_STP(); // Creamos las conexiones para intercambio de señalizacion del protocolo STP
					generoTimers(); // Timer Generacion PUBLISH - Timer Muestra Listas
			}
		});
		timerGeneracionConexiones.start();
		timerGeneracionConexiones.setRepeats(false);
	}

	public void generoTimers() {
		timerGeneracionPUB = new Timer(10000, new ActionListener() // Cada 10s
		{
			public void actionPerformed(ActionEvent e) {

					generacionPUBLISH();

			}
		});
		timerGeneracionPUB.start();
		timerGeneracionPUB.setRepeats(true);

		timerMuestraLista = new Timer(40000, new ActionListener() // Cada 40s
		{
			public void actionPerformed(ActionEvent e) {
				System.out.println("");
				System.out.println("Informacion periodica");
				System.out.println("Mi ROOT es: " + brokerROOT.root_ID + " con puerto: " + brokerROOT.root_port);
				System.out.println("Mi puerto ROOT es: " + brokerROOT.my_port);
				mostrar_lista_DESIGNADOS();
				mostrar_lista_BLOQUEADOS();
				System.out.println("");
			}
		});
		timerMuestraLista.start();
		timerMuestraLista.setRepeats(true);
	}

	public int calcularCapacidad(int L, int R) {
		int _C = ALFA * L + BETA * R;
		return _C;
	}

	public void inicioProtocolo() {

		System.setProperty("user.timezone", "WET");

		int L = 20; // Datos aleatorios
		int R = 100; // Datos aleatorios

		/* Inicio Protocolo STP */
		// Al inicio del protocolo, me establezco a mi mismo como ROOT

		miBroker.my_ID = inforInput.ID;
		miBroker.my_port = inforInput.port;
		miBroker.my_address = inforInput.address;
		miBroker.my_C = inforInput.C;
		miBroker.root_ID = inforInput.ID;
		miBroker.root_port = inforInput.port;
		miBroker.root_address = inforInput.address;
		miBroker.root_C = miBroker.my_C;
		miBroker.RTT = 0;
		// int C = calcularCapacidad(L, R);

		// Todos los puertos son marcados como DESIGNADOS
		for (int j = 0; j < inforInput.brokersMQTTExternos.size(); j++) {
			puertos_DESIGNADOS.add(inforInput.brokersMQTTExternos.get(j).split(" ")[1].split(":")[1]);
		}

		brokerROOT = miBroker; // Soy ROOT
	}

	public void generacionPUBLISH() {

		System.out.println("---------------- Genero Mensaje ----------------");

		if (!puertos_DESIGNADOS.isEmpty()) { // Si la lista de Proxys no esta vacia
			// Contenido del primer mensaje
			long TInicio = System.currentTimeMillis(); // Determina el tiempo de ejecucion

			String[] msg = new String[] { miBroker.my_port, miBroker.my_address, String.valueOf(miBroker.my_C),
					miBroker.root_ID, miBroker.root_port, miBroker.root_address, String.valueOf(miBroker.root_C), Long.toString(miBroker.RTT), Long.toString(TInicio) };
			StringJoiner firstmessage = new StringJoiner(" ");
			for (String s : msg)
				firstmessage.add(s);

			byte[] bytes = (firstmessage.toString()).getBytes(Charset.defaultCharset());

			for (int i = 0; i < puertos_DESIGNADOS.size(); i++) {
				for (int j = 0; j < conexiones_STP.size(); j++) {
					if (puertos_DESIGNADOS.get(i).equalsIgnoreCase(conexiones_STP.get(j).port)){
						//Busco la conexion MQTT del puerto DESIGNADO

						try { // Genero el mensaje
							MqttMessage message = new MqttMessage(bytes);
							message.setQos(1);
							conexiones_STP.get(j).cliente.publish("STP", message);
						} catch (MqttException me) {
							System.out.println("Error Generacion PUBLISH - STP: " + me.getClass().getName() + "\n" + "Mensaje: "  + me.getMessage());
							System.out.println(me.getReasonCode());
							// System.out.println(me.getMessage());
							System.out.println(me.getLocalizedMessage());
							System.out.println(me.getCause());
							System.out.println("Broker Error Generacion PUBLISH - STP: " + me);

						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}	
			}
		}
	}

	public void algoritmoSTP (Broker brokerEmisor, Long TInicio) {
		// Seleccion del ROOT por el nodo con mayor Capacidad, en caso de tener igual Capacidad, sera ROOT el de menor ID

		// Calculo el RTT del camino
		long TFin = System.currentTimeMillis();
		long RTT_enlace = 2*(TFin - TInicio);
		brokerEmisor.RTT = brokerEmisor.RTT + RTT_enlace;
		brokerEmisor.RTT = media_movil(0.125, brokerEmisor);
		System.out.println("El RTT del camino total es: " + Long.toString(brokerEmisor.RTT));
		
		// Seleccion del Broker ROOT
		if (miBroker.root_C < brokerEmisor.root_C) {
			// EL ROOT del broker Emisor es mejor, por lo tanto pasa a ser mi ROOT

			if (brokerROOT.my_port.equalsIgnoreCase(miBroker.my_port)){ //Si yo era antes el ROOT
				timerGeneracionPUB.stop(); // Dejo de generar mensajes
			} else {
				puertos_DESIGNADOS.add(brokerROOT.my_port); // Anterior puerto ROOT
			}

			if (esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
				puertos_DESIGNADOS.remove(brokerEmisor.my_port);
			}

			if (esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
				puertos_BLOQUEADOS.remove(brokerEmisor.my_port);
			}

			// Actualizo los datos de mi broker ROOT
			actualizo_datos_STP(brokerEmisor);
			brokerROOT = brokerEmisor; //Mi puerto ROOT es el Emisor 
		}


		if (miBroker.root_C == brokerEmisor.root_C) {
			// Ambos brokers ROOT tienen la misma capacidad, elegimos al broker que tenga
			// menor ID
			String[] id_myRoot = miBroker.root_ID.split("-");
			String[] id_rootEmisor = brokerEmisor.root_ID.split("-");

			if (Integer.parseInt(id_myRoot[1]) > Integer.parseInt(id_rootEmisor[1])) { 
				// EL ROOT del broker Emisor es mejor, por lo tanto pasa a ser mi ROOT
				
				if (brokerROOT.my_port.equalsIgnoreCase(miBroker.my_port)){ //Si yo era antes el ROOT
					timerGeneracionPUB.stop(); // Dejo de generar mensajes
				} else {
					puertos_DESIGNADOS.add(brokerROOT.my_port); // Anterior puerto ROOT
				}
			
				if (esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
					puertos_DESIGNADOS.remove(brokerEmisor.my_port);
				}

				if (esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
					puertos_BLOQUEADOS.remove(brokerEmisor.my_port);
				}

				// Actualizo los datos de mi broker ROOT
				actualizo_datos_STP(brokerEmisor);
				brokerROOT = brokerEmisor; //Mi puerto ROOT es el Emisor 
			} 
			
			if (Integer.parseInt(id_myRoot[1]) == Integer.parseInt(id_rootEmisor[1])) { 	
				// Tenemos el mismo ROOT
	
				if (brokerROOT.RTT > brokerEmisor.RTT){ // Tarda menos por su camino
	
					puertos_DESIGNADOS.add(brokerROOT.my_port); // Anterior puerto ROOT					

					if (esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
						puertos_DESIGNADOS.remove(brokerEmisor.my_port);
					}

					if (esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
						puertos_BLOQUEADOS.remove(brokerEmisor.my_port);
					}

					// Actualizo los datos de mi broker ROOT
					actualizo_datos_STP(brokerEmisor);
					brokerROOT = brokerEmisor; //Mi puerto ROOT es el Emisor 
				} 
				
				if (brokerROOT.RTT == brokerEmisor.RTT) { 
					// Igual distancia, elijo el que tiene mayor cantidad de recursos (mayor C)
				
					if (brokerROOT.my_C < brokerEmisor.my_C) {
						// aqui he quitado lo de generar mensajes	

						puertos_DESIGNADOS.add(brokerROOT.my_port); // Anterior puerto ROOT

						if (esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
							puertos_DESIGNADOS.remove(brokerEmisor.my_port);
						}

						if (esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
							puertos_BLOQUEADOS.remove(brokerEmisor.my_port);
						}

						// Actualizo los datos de mi broker ROOT
						actualizo_datos_STP(brokerEmisor);
						brokerROOT = brokerEmisor; //Mi puerto ROOT es el Emisor 
					}

					if (brokerROOT.my_C == brokerEmisor.my_C){
						String[] id_my = brokerROOT.my_ID.split("-"); // aqui lo he cambiado, ojo
						String[] id_Emisor = brokerEmisor.my_ID.split("-");

						if (Integer.parseInt(id_my[1]) > Integer.parseInt(id_Emisor[1])) {
							// aqui he quitado lo de generar mensajes	
									
							puertos_DESIGNADOS.add(brokerROOT.my_port); // Anterior puerto ROOT
							
						
							if (esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
								puertos_DESIGNADOS.remove(brokerEmisor.my_port);
							}

							if (esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
								puertos_BLOQUEADOS.remove(brokerEmisor.my_port);
							}

							// Actualizo los datos de mi broker ROOT
							actualizo_datos_STP(brokerEmisor);
							brokerROOT = brokerEmisor; //Mi puerto ROOT es el Emisor 
						} 
					}
				}
			}
		}

		InforBPDU newBPDU = new InforBPDU (brokerEmisor.my_ID, brokerEmisor.my_port, RTT_enlace, brokerEmisor.RTT, brokerEmisor);

		if (brokerROOT.my_port.equalsIgnoreCase(brokerEmisor.my_port) && brokerROOT.root_port.equalsIgnoreCase(brokerEmisor.my_port)){
			// EL mensaje viene directamente del broker ROOT

			//Guardo/Actualizo la informacion del BPDU recibido en la lista 
			if (!inforBPDU.isEmpty() && !listaTimeOuts.isEmpty()){
				for (int b=0; b < inforBPDU.size(); b++){ // Actualizamos la informacion 
					if (inforBPDU.get(b).port.equalsIgnoreCase(brokerEmisor.my_port)) {
						inforBPDU.remove(inforBPDU.get(b));
						break;
					}
				}

				for (int x=0; x < inforBPDUTotal.size(); x++){ // Actualizamos la informacion 
					if (inforBPDUTotal.get(x).port.equalsIgnoreCase(brokerEmisor.my_port)) {
						inforBPDUTotal.remove(inforBPDUTotal.get(x));
						break;
					}
				}

				// Actualizo el Time Out		
				for (int t=0; t < listaTimeOuts.size(); t++){
					if( listaTimeOuts.get(t).port.equalsIgnoreCase(brokerEmisor.my_port)){
						
						listaTimeOuts.get(t).scheduler.shutdown(); // Paro el TimeOut
						listaTimeOuts.remove(listaTimeOuts.get(t));
						break;
					}
				}
				inforBPDU.add(newBPDU);
				generoTimeOut(brokerEmisor.my_port);
				mostrar_lista_BPDUs();

			} else {
				inforBPDU.add(newBPDU);
				generoTimeOut(brokerEmisor.my_port);
				mostrar_lista_BPDUs();
			}

		}


		if (brokerROOT.my_port.equalsIgnoreCase(brokerEmisor.my_port) && !brokerROOT.root_port.equalsIgnoreCase(miBroker.my_port)){ 
			// Si la informacion me llega por el Puerto ROOT, se la reenvio a mis puertos DESIGNADOS
			reenvio_informacion(brokerEmisor);	

			//Guardo/Actualizo la informacion del BPDU recibido en la lista 
			if (!inforBPDU.isEmpty() && !listaTimeOuts.isEmpty()){
				for (int b=0; b < inforBPDU.size(); b++){ // Actualizamos la informacion 
					if (inforBPDU.get(b).port.equalsIgnoreCase(brokerEmisor.my_port)) {
						inforBPDU.remove(inforBPDU.get(b));
						break;
					}
				}

				for (int x=0; x < inforBPDUTotal.size(); x++){ // Actualizamos la informacion 
					if (inforBPDUTotal.get(x).port.equalsIgnoreCase(brokerEmisor.my_port)) {
						inforBPDUTotal.remove(inforBPDUTotal.get(x));
						break;
					}
				}

				// Actualizo el Time Out		
				for (int t=0; t < listaTimeOuts.size(); t++){
					if( listaTimeOuts.get(t).port.equalsIgnoreCase(brokerEmisor.my_port)){
						
						listaTimeOuts.get(t).scheduler.shutdown(); // Paro el TimeOut
						listaTimeOuts.remove(listaTimeOuts.get(t));
						break;
					}
				}
				inforBPDU.add(newBPDU);
				generoTimeOut(brokerEmisor.my_port);
				mostrar_lista_BPDUs();
			} else {
				inforBPDU.add(newBPDU);
				generoTimeOut(brokerEmisor.my_port);
				mostrar_lista_BPDUs();
			}

		}


		if (!brokerROOT.root_port.equalsIgnoreCase(miBroker.my_port) && !brokerROOT.my_port.equalsIgnoreCase(brokerEmisor.my_port) && miBroker.root_port.equalsIgnoreCase(brokerEmisor.root_port)) {
			// Si yo no soy el brokerROOT y el mensaje que me llega no es del RP y ambos tenemos mismo ROOT
		
			//Guardo/Actualizo la informacion del BPDU recibido en la lista 
			if (!inforBPDU.isEmpty() && !listaTimeOuts.isEmpty()){
				for (int b=0; b < inforBPDU.size(); b++){ // Actualizamos la informacion 
					if (inforBPDU.get(b).port.equalsIgnoreCase(brokerEmisor.my_port)) {
						inforBPDU.remove(inforBPDU.get(b));
						break;
					}
				}

				for (int x=0; x < inforBPDUTotal.size(); x++){ // Actualizamos la informacion 
					if (inforBPDUTotal.get(x).port.equalsIgnoreCase(brokerEmisor.my_port)) {
						inforBPDUTotal.remove(inforBPDUTotal.get(x));
						break;
					}
				}

				// Actualizo el Time Out		
				for (int t=0; t < listaTimeOuts.size(); t++){
					if( listaTimeOuts.get(t).port.equalsIgnoreCase(brokerEmisor.my_port)){
						//if( listaTimeOuts.get(t).scheduler!= null){
							listaTimeOuts.get(t).scheduler.shutdown(); // Paro el TimeOut
							listaTimeOuts.remove(listaTimeOuts.get(t));
						//}
							
						break;
					}
				}
				inforBPDU.add(newBPDU);
				inforBPDUTotal.add(newBPDU);
				generoTimeOut(brokerEmisor.my_port);
				mostrar_lista_BPDUs();
			} else {
				inforBPDU.add(newBPDU);
				inforBPDUTotal.add(newBPDU);
				generoTimeOut(brokerEmisor.my_port);
				mostrar_lista_BPDUs();
			}	

			long lo_que_me_cuesta_enviarle_el_mensaje =  RTT_enlace + brokerROOT.RTT;

			if ( lo_que_me_cuesta_enviarle_el_mensaje > brokerEmisor.RTT ){
				// Bloqueo el puerto
				if (esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
					puertos_DESIGNADOS.remove(brokerEmisor.my_port);
				}
				if (!esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
					puertos_BLOQUEADOS.add(brokerEmisor.my_port);
				}
			} else if ( lo_que_me_cuesta_enviarle_el_mensaje < brokerEmisor.RTT ){
				// Desbloqueo el puerto
				if (esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
					puertos_BLOQUEADOS.remove(brokerEmisor.my_port);
				}
				if (!esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
					puertos_DESIGNADOS.add(brokerEmisor.my_port);
				}
			} else if (lo_que_me_cuesta_enviarle_el_mensaje == brokerEmisor.RTT){
				// El coste del camino es el mismo para ambos, bloqueo si mi ID es mayor

				if (Integer.parseInt(miBroker.my_ID.split("-")[1]) > Integer.parseInt(brokerEmisor.my_ID.split("-")[1]) ){
					// Bloqueo el puerto
					if (esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
						puertos_DESIGNADOS.remove(brokerEmisor.my_port);
					}
					if (!esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
						puertos_BLOQUEADOS.add(brokerEmisor.my_port);
					}
				}else if (Integer.parseInt(miBroker.my_ID.split("-")[1]) < Integer.parseInt(brokerEmisor.my_ID.split("-")[1]) ){
					// Desbloqueo el puerto
					if (esta_en_la_lista(puertos_BLOQUEADOS, brokerEmisor.my_port)) {
						puertos_BLOQUEADOS.remove(brokerEmisor.my_port);
					}
					if (!esta_en_la_lista(puertos_DESIGNADOS, brokerEmisor.my_port)) {
						puertos_DESIGNADOS.add(brokerEmisor.my_port);
					}
				}
			}	
		}
	} // Fin algoritmo STP

	public boolean esta_en_la_lista(ArrayList<String> lista, String port) {
		// Comprueba que el puerto esta en la lista

		boolean en_lista = false;

		if(!lista.isEmpty()){			
			for (int i = 0; i < lista.size(); i++) {
				if (lista.get(i).equalsIgnoreCase(port)) {
					en_lista = true;
					break;
				}
			}
		}
		return en_lista;
	}

	public long media_movil (double alpha, Broker brokerEmisor){

		// Buscamos la media anterior
		long newEstimatedRTT = 0;
		long EstimatedRTT = 0;

		if (!inforBPDUTotal.isEmpty()){
			for (int b=0; b < inforBPDUTotal.size(); b++){ // Actualizamos la informacion 
				if (inforBPDUTotal.get(b).port.equalsIgnoreCase(brokerEmisor.my_port)) {
					EstimatedRTT = inforBPDUTotal.get(b).coste_hasta_el_ROOT;
				}
			}
		}
		
		if (newEstimatedRTT == 0)
			newEstimatedRTT = brokerEmisor.RTT;
		else
			newEstimatedRTT = Math.round((1-alpha)*EstimatedRTT + alpha*brokerEmisor.RTT);
		
		return newEstimatedRTT;
	}

	public void actualizo_datos_STP(Broker _brokerEmisor) {
		
		// Actualiza los datos del broker ROOT
		miBroker.root_ID = _brokerEmisor.root_ID;
		miBroker.root_port = _brokerEmisor.root_port;
		miBroker.root_address = _brokerEmisor.root_address;
		miBroker.root_C = _brokerEmisor.root_C;
		miBroker.RTT = _brokerEmisor.RTT;
	}

	public void mostrar_lista_BLOQUEADOS() {
		// Muestra por pantalla los puertos bloqueados
		System.out.println("Los puertos BLOQUEADOS son los siguientes:");
		if (!puertos_BLOQUEADOS.isEmpty()) {
			for (int i = 0; i < puertos_BLOQUEADOS.size(); i++) {
				System.out.println(puertos_BLOQUEADOS.get(i));
			}
		} else
			System.out.println("La lista esta vacia.");

		System.out.println("");
	}

	public void mostrar_lista_DESIGNADOS() {
		// Muestra por pantalla los puertos designados
		System.out.println("Los puertos DESIGNADOS son los siguientes:");
		if (!puertos_DESIGNADOS.isEmpty()) {
			for (int i = 0; i < puertos_DESIGNADOS.size(); i++) {
				System.out.println(puertos_DESIGNADOS.get(i));
			}
		} else
			System.out.println("La lista esta vacia.");

		System.out.println("");
	}

	public void mostrar_lista_BPDUs() {
		// Muestra por pantalla la lista de los BPDUs
		
		if (!inforBPDU.isEmpty()) {
			System.out.println("");
			System.out.println("TABLA DE ENCAMINAMIENTO:");
			System.out.println("");
			System.out.printf("|    ID    |   Puerto | Coste  enlace | Coste al ROOT |");
			
			for (int i = 0; i < inforBPDU.size(); i++) {
				
				System.out.println("");
				System.out.println("|----------|----------|---------------|---------------|");
				System.out.printf("|%10s|%10s|%15d|%15d|\n",inforBPDU.get(i).ID, inforBPDU.get(i).port, inforBPDU.get(i).coste_enlace, inforBPDU.get(i).coste_hasta_el_ROOT);
			}
			System.out.println("|----------|----------|---------------|---------------|");
			System.out.println("");
		}
	}
	

	public void reenvio_informacion (Broker _brokerEmisor) {

		System.out.println("");
		System.out.println(" --- Reenvio informacion ---");
		System.out.println("");

		if (!puertos_DESIGNADOS.isEmpty()) { // Si la lista de puertos DESIGNADOS no esta vacia
			// Contenido del primer mensaje
			long TInicio = System.currentTimeMillis(); // Determina el tiempo de ejecucion

			String[] msg = new String[] {miBroker.my_port, miBroker.my_address, String.valueOf(miBroker.my_C),
					_brokerEmisor.root_ID, _brokerEmisor.root_port, _brokerEmisor.root_address, String.valueOf(_brokerEmisor.root_C), String.valueOf(_brokerEmisor.RTT),  Long.toString(TInicio) };
			StringJoiner firstmessage = new StringJoiner(" ");
			for (String s : msg)
				firstmessage.add(s);

			byte[] bytes = (firstmessage.toString()).getBytes(Charset.defaultCharset());

			for (int i = 0; i <  puertos_DESIGNADOS.size(); i++) {

				if (puertos_DESIGNADOS.get(i).equalsIgnoreCase(_brokerEmisor.my_port)){
					continue;
				} else {
					for (int j = 0; j < conexiones_STP.size(); j++) {
						if (puertos_DESIGNADOS.get(i).equalsIgnoreCase(conexiones_STP.get(j).port)){
						
							try { // Reenvio del mensaje
								MqttMessage message = new MqttMessage(bytes);
								message.setQos(1);
								conexiones_STP.get(j).cliente.publish("STP", message);
							} catch (MqttException me) {
								System.out.println("Error Reenvio PUBLISH - STP: " + me.getClass().getName() + "\n" + "Mensaje: " + me.getMessage());
								System.out.println(me.getReasonCode());
								// System.out.println(me.getMessage());
								System.out.println(me.getLocalizedMessage());
								System.out.println(me.getCause());
								System.out.println(me);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
							break;
						}
					}
				}									
			}					
		}
	}		

	public void creacion_conexiones_STP () {

		if (!inforInput.brokersMQTTExternos.isEmpty()) { // Si la lista con todos los Proxys no esta vacia

			for (int i = 0; i < inforInput.brokersMQTTExternos.size(); i++) {

				String broker = ((String) inforInput.brokersMQTTExternos.get(i)).split(" ")[1]; 

				try { 
					MqttClient client = new MqttClient("tcp://" + broker, miBroker.my_ID, new MemoryPersistence());
					client.connect();
					ClienteMQTT newClient = new ClienteMQTT( ((String) inforInput.brokersMQTTExternos.get(i)).split(" ")[0] , broker.split(":")[1] , client);
					conexiones_STP.add(newClient);

					//System.out.println ("Conexion creada para: " + conexiones_STP.get(i).ID);
				} catch (MqttException me) {
					System.out.println("Error Generacion Cliente STP" + me.getClass().getName() + "\n" + "Mensaje: "  + me.getMessage());
					System.out.println(me.getReasonCode());
					// System.out.println(me.getMessage());
					System.out.println(me.getLocalizedMessage());
					System.out.println(me.getCause());
					System.out.println("Broker Error Generacion Cliente - STP: " + me);

				} catch (Exception ex) {
					ex.printStackTrace();
				}				
			}
		} 
	}


	public void generoTimeOut(String port) {
		
		ScheduledExecutorService newScheduler = Executors.newScheduledThreadPool(1);
		System.out.println("");

		System.out.println("Actualizo el temporizador del puerto " + port);
		System.out.println("");
				
				
		Runnable runnable = new Runnable() {
			String _port = port;
			String anteriorROOT = miBroker.root_port;
		    int countdownStarter = 20; // Numero de segundos
					
		    public void run() {						
				
				//System.out.println("Temporizador  - Puerto: " + _port + " Cuentraatras: " + String.valueOf(countdownStarter));
						
		        //System.out.println("Hola soy runnable " + " el numero de ejecucion es: " + countdownStarter);
		        countdownStarter--;

		        if (countdownStarter < 0) {
					newScheduler.shutdown();

		          
					System.out.println("Ha caido el TimeOut: " + _port);						

					//Elimino esta entrada de la tabla 
					if (!inforBPDU.isEmpty()){
						for (int b=0; b < inforBPDU.size(); b++){ // Actualizamos la informacion 
							if (inforBPDU.get(b).port.equalsIgnoreCase(_port)) {
								inforBPDU.remove(inforBPDU.get(b));
								break;
							}							
						}
					}

					// Elimino el Time-Out de la lista
					if (!listaTimeOuts.isEmpty()){			
						for (int t=0; t < listaTimeOuts.size(); t++){
							if (listaTimeOuts.get(t).port.equalsIgnoreCase(_port)){
								listaTimeOuts.remove(listaTimeOuts.get(t));									
								break;
							}
						}
					}

					if (_port.equalsIgnoreCase(brokerROOT.my_port)){ // Ha caido el puerto ROOT, busco uno nuevo
							
						brokerROOT = miBroker;
						brokerROOT.root_ID = miBroker.my_ID;
						brokerROOT.root_address = miBroker.my_address;
						brokerROOT.root_port = miBroker.my_port;
						brokerROOT.root_C = miBroker.my_C;
						brokerROOT.RTT = 0;
					}

					// Han expirado todas las entradas de la tabla, por lo tanto hay que buscar un nuevo ROOT
					if (listaTimeOuts.isEmpty()){	
						System.out.println("Elegimos ROOT de nuevo ");		
										
						System.out.println("ROOT anterior: " + anteriorROOT);

						miBroker.root_ID = miBroker.my_ID;
						miBroker.root_address = miBroker.my_address;
						miBroker.root_port = miBroker.my_port;
						miBroker.root_C = miBroker.my_C;
						miBroker.RTT = 0;

						brokerROOT = miBroker;

						puertos_DESIGNADOS.clear();
						puertos_BLOQUEADOS.clear();

						// Todos los puertos son marcados como DESIGNADOS
						for (int j = 0; j < inforInput.brokersMQTTExternos.size(); j++) {
							if (inforInput.brokersMQTTExternos.get(j).split(" ")[1].split(":")[1].equalsIgnoreCase(anteriorROOT)){
								continue;
							}else {
								puertos_DESIGNADOS.add(inforInput.brokersMQTTExternos.get(j).split(" ")[1].split(":")[1]);
							}
						}

						timerGeneracionPUB.start(); // Comienza a generar mensajes de nuevo 
					}
				}
			}
		};
		        
		newScheduler.scheduleAtFixedRate(runnable, 0, 1, SECONDS);
		InforTimeOut newTimeOut = new InforTimeOut(port, newScheduler);
		listaTimeOuts.add(newTimeOut);	
	}

}//fin del programa
