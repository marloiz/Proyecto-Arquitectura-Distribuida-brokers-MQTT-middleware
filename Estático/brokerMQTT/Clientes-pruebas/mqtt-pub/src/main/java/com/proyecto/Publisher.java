package com.proyecto;

import java.lang.System;
import java.lang.Thread;
import java.lang.InterruptedException;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
/**
 * Hello world!
 *
 */
public class Publisher 
{

    private static int qos = 0;
    private String topic = "timestamp";
    private MqttClient client;

    public Publisher(){

        try{

            String host = String.format("tcp://%s:%d", "IP", "puerto (int)"); //poner IP y puertos¡ del servidor

            String clientId = "MQTT-Java-Example-PUB";


            MqttConnectOptions conOpt = new MqttConnectOptions();
            conOpt.setCleanSession(true);

            this.client = new MqttClient(host, clientId, new MemoryPersistence());
            this.client.connect(conOpt);

            while(true){
                String contenido = String.valueOf(System.currentTimeMillis());	
                MqttMessage message = new MqttMessage(contenido.getBytes());
                message.setQos(qos);


                this.client.publish(this.topic, message);
        
                System.out.println("Timestamp: " + contenido + " PUBLICADO");
            
                Thread.sleep(2000);
            }
        }catch(MqttException | InterruptedException e){
        }
    }

    public static void main( String[] args )
    {	
        if(args.length != 0){
            qos = Integer.parseInt(args[0]);
        }else{
            System.out.println("ERROR falta QoS");
                System.exit(0);
        }
        Publisher pub = new Publisher();

    }
}
