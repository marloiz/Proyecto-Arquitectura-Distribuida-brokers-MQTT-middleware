package com.proyecto;

import java.util.ArrayList;
import java.util.List;

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

import javax.swing.*;
import javax.swing.Timer;
import java.awt.event.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.Thread;

public class ProtocoloInteraccionMqtt implements ProtocoloInteraccion {

	private GestorRecursos gestorRecursos;
	private Semaphore mutex;
	private Boolean sec;
	private InforInput inforInput;
	private ProtocoloSTP protocoloSTP;
	public ArrayList<ClienteMQTT> conexiones_Datos = new ArrayList<ClienteMQTT>(); // Lista con todas las conexiones para transmitir los BPDUs

	public ProtocoloInteraccionMqtt(GestorRecursos _gestorRecursos, Semaphore _mutex, Boolean _sec,
			InforInput _inforInput, ProtocoloSTP _protocoloSTP) {

		gestorRecursos = _gestorRecursos;
		mutex = _mutex;
		sec = _sec;
		inforInput = _inforInput;
		protocoloSTP = _protocoloSTP;

	}

	public Boolean iniciarComunicacion(InterceptPublishMessage pubMsg, String tipoAccion) {

		if (pubMsg != null) { // gestiono un PUBLISH

			String name = pubMsg.getTopicName(); // TOPIC
			String content = new String(pubMsg.getPayload().array()); // Contenido del Mensaje
			String idTransmisor = pubMsg.getClientID();
			Boolean puertoBLOQUEADO = false;

			String[] parts = idTransmisor.split("-"); 
			String idProxy = parts[0];

			try {

				// Compruebo si el mensaje viene de un puerto BLOQUEADO
				if (!protocoloSTP.puertos_BLOQUEADOS.isEmpty()){
					for (int i = 0; i < protocoloSTP.puertos_BLOQUEADOS.size(); i++) {

						if (!inforInput.brokersMQTTExternos.isEmpty()) { // Si la lista de Proxys no esta vacia
							for (int j = 0; j < inforInput.brokersMQTTExternos.size(); j++) { 
					
								String idProxyLista = ((String) inforInput.brokersMQTTExternos.get(j)).split(" ")[0]; // me guardo el id de cada uno de la lista
									
								if (idProxyLista.equalsIgnoreCase(idTransmisor)){  
									//esto me sirve para STP, no para mensajes normales
									System.out.println("Mensaje de: " + protocoloSTP.puertos_BLOQUEADOS.get(i));
									System.out.println("Puertos Bloqueados: " + protocoloSTP.puertos_BLOQUEADOS.get(i));
									puertoBLOQUEADO = true;
									break;
								}
							}
						}
					}
				}
						
				if (name.equalsIgnoreCase("STP") && idProxy.equalsIgnoreCase("Proxy")) {
					// Necesito procesar todos los mensajes de STP, ya sean del puerto raiz o de puerdos designados o bloqueados
					mutex.acquire();
					System.out.println("");
					System.out.println("Me llega un mensaje STP");

					STP(idTransmisor, content);

				} else {
					if (!puertoBLOQUEADO){
						mutex.acquire();
						System.out.println("");
						System.out.println("Me llega un mensaje que no es STP");
						propagarProxys(idTransmisor, name, content);
					}	
				}
			} catch (InterruptedException e) {
			}

		}
		return true;
	}

	public void propagarProxys(String _idTransmisor, String _name, String _content) {

		String[] parts = _idTransmisor.split("-"); // Para ver si viene de proxy

		String idProxy = parts[0];

		System.out.println("idTransmisor: " + _idTransmisor);

		if (idProxy.equalsIgnoreCase("Proxy")) { // PUBLISH recibido de un Broker

			if (!inforInput.brokersMQTTExternos.isEmpty()) { // Si la lista de Proxys no esta vacia

                for (int i = 0; i < protocoloSTP.conexiones_STP.size(); i++) {

                    // Lo mando por el Puerto Root
					if (protocoloSTP.conexiones_STP.get(i).ID.equalsIgnoreCase(protocoloSTP.brokerROOT.my_ID) && !protocoloSTP.brokerROOT.my_ID.equalsIgnoreCase(protocoloSTP.miBroker.my_ID)) { 
						// Compruebo que la conexion pertenece al puerto Root
						// y yo no soy el Broker ROOT (porque sino me lo mando a mi mismo)

						if (_idTransmisor.equalsIgnoreCase(protocoloSTP.conexiones_STP.get(i).ID)) { 
							// Compruebo si es el que me ha mandado el mensaje es este broker 
							// No se lo reenvio

							System.out.println("Viene del puerto RAIZ, no se lo mando a el.");
							continue;
						} else {
							try { // Reenvio del mensaje
								MqttMessage message = new MqttMessage(_content.getBytes());
								message.setQos(0);
								protocoloSTP.conexiones_STP.get(i).cliente.publish(_name, message);
							} catch (MqttException me) {
								System.out.println("Error Reenvio PUBLISH - Datos: " + me.getClass().getName() + "\n" + "Mensaje: " + me.getMessage());
								System.out.println(me.getReasonCode());
							// System.out.println(me.getMessage());
								System.out.println(me.getLocalizedMessage());
								System.out.println(me.getCause());
								System.out.println(me);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						
                    } else{ // Resto de puertos, miramos los Designados
                        if (!protocoloSTP.puertos_DESIGNADOS.isEmpty()) { // Si la lista de puertos DESIGNADOS no esta vacia
							if (_idTransmisor.equalsIgnoreCase(protocoloSTP.conexiones_STP.get(i).ID)) { 
							// Compruebo si es el que me ha mandado el mensaje es este broker 
							// No se lo reenvio
								continue;
							} else {
								for (int j = 0; j < protocoloSTP.puertos_DESIGNADOS.size(); j++) {

									if (protocoloSTP.puertos_DESIGNADOS.get(j).equalsIgnoreCase(protocoloSTP.conexiones_STP.get(i).port) ) {

										try { // Reenvio del mensaje
											MqttMessage message = new MqttMessage(_content.getBytes());
											message.setQos(0);
											protocoloSTP.conexiones_STP.get(i).cliente.publish(_name, message);
										} catch (MqttException me) {
											System.out.println("Error Reenvio PUBLISH - Datos: " + me.getClass().getName() + "\n" + "Mensaje: " + me.getMessage());
											System.out.println(me.getReasonCode());
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
            }

		} else { // El mensaje lo hemos recibido de un cliente
					// Lo reenviamos a todos los puertos Designados y al puerto ROOT

			if (!inforInput.brokersMQTTExternos.isEmpty()) { // Si la lista de Proxys no esta vacia

                for (int i = 0; i < protocoloSTP.conexiones_STP.size(); i++) {

                    // Lo mando por el Puerto Root
					if (protocoloSTP.conexiones_STP.get(i).ID.equalsIgnoreCase(protocoloSTP.brokerROOT.my_ID) && !protocoloSTP.brokerROOT.my_ID.equalsIgnoreCase(protocoloSTP.miBroker.my_ID)) { 
						// Compruebo que la conexion pertenece al puerto Root
						// y yo no soy el Broker ROOT (porque sino me lo mando a mi mismo)

						try { // Reenvio del mensaje
							MqttMessage message = new MqttMessage(_content.getBytes());
							message.setQos(0);
							protocoloSTP.conexiones_STP.get(i).cliente.publish(_name, message);
						} catch (MqttException me) {
							System.out.println("Error Reenvio PUBLISH - Datos: " + me.getClass().getName() + "\n" + "Mensaje: " + me.getMessage());
							System.out.println(me.getReasonCode());
						// System.out.println(me.getMessage());
							System.out.println(me.getLocalizedMessage());
							System.out.println(me.getCause());
							System.out.println(me);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						
                    } else{ // Resto de puertos, miramos los Designados
                        if (!protocoloSTP.puertos_DESIGNADOS.isEmpty()) { // Si la lista de puertos DESIGNADOS no esta vacia
                            for (int j = 0; j < protocoloSTP.puertos_DESIGNADOS.size(); j++) {

                                if (protocoloSTP.puertos_DESIGNADOS.get(j).equalsIgnoreCase(protocoloSTP.conexiones_STP.get(i).port) ) {
									try { // Reenvio del mensaje
										MqttMessage message = new MqttMessage(_content.getBytes());
										message.setQos(0);
										protocoloSTP.conexiones_STP.get(i).cliente.publish(_name, message);
									} catch (MqttException me) {
										System.out.println("Error Reenvio PUBLISH - Datos: " + me.getClass().getName() + "\n" + "Mensaje: " + me.getMessage());
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
        }
		mutex.release();
	}

	public void STP(String _idTransmisor, String _content) {
		System.out.println("Llega el mensaje STP de " + _idTransmisor);

		/* Lectura de datos */
		String[] parts = _content.split(" ");
		System.out.println(_content);
		Broker brokerEmisor = new Broker(_idTransmisor, parts[0], parts[1], Integer.parseInt(parts[2]), parts[3],
				parts[4], parts[5], Integer.parseInt(parts[6]), Integer.parseInt(parts[7]));
		protocoloSTP.algoritmoSTP(brokerEmisor);

		mutex.release();
	}
}
