package com.proyecto;

import io.moquette.server.config.ClasspathResourceLoader;
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
import io.moquette.interception.messages.InterceptConnectionLostMessage;

import com.proyecto.GestorRecursos;


public class PublisherListener extends AbstractInterceptHandler {

    public ProtocoloInteraccionMqtt protocoloInteraccionMqtt;

    public PublisherListener(ProtocoloInteraccionMqtt _protocoloInteraccionMqtt){

	protocoloInteraccionMqtt = _protocoloInteraccionMqtt;

    }

    @Override
    public void onPublish(InterceptPublishMessage message) {
	
	// Recepcion de un mensaje MQTT PUBLISH			
	protocoloInteraccionMqtt.iniciarComunicacion(message, null);
    }

/*
    @Override
    public void onSubscribe(InterceptSubscribeMessage message) {

	// Recepcion de un mensaje MQTT SUBSCRIBE
	protocoloInteraccionMqtt.iniciarComunicacion(null, message, null, null, null, null);
    }
*/
/*
    @Override
    public void onConnectionLost(InterceptConnectionLostMessage message){

	// notifico la desconexion del cliente mqtt al gestor de recursos
	if(!message.getClientID().equalsIgnoreCase("Broker")){

		protocoloInteraccionMqtt.cancelarComunicacion(message.getClientID());

	}
    }
*/
	
}
