package com.proyecto;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.URI;
import java.net.URISyntaxException;

import java.io.*;

/**
 * A sample application that demonstrates how to use the Paho MQTT v3.1 Client blocking API.
 */
public class Subscriber implements MqttCallback {

    private static int qos = 0;
    private String topic = "timestamp";
    private MqttClient client;
    private static FileWriter fichero;
    private PrintWriter pw;

    public Subscriber(String nombreFichero) throws MqttException, URISyntaxException {

	try{
        	fichero = new FileWriter("src/main/output/"+nombreFichero);
       		pw = new PrintWriter(fichero);
	}catch(Exception e){
	}

	iniciarSubscripcion();
    }

    public void iniciarSubscripcion() throws MqttException {

        String host = String.format("tcp://%s:%d", "IP", "puert (int)"); //poner IP y puerto del servidor

        String clientId = "MQTT-Java-Example";
       
        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(true);

        this.client = new MqttClient(host, clientId, new MemoryPersistence());
        this.client.setCallback(this);
        this.client.connect(conOpt);

        this.client.subscribe(this.topic, qos);
    }

    private String[] getAuth(URI uri) {
        String a = uri.getAuthority();
        String[] first = a.split("@");
        return first[0].split(":");
    }

    public void sendMessage(String payload) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(qos);
        this.client.publish(this.topic, message); // Blocking publish
    }

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost because: " + cause);
        System.exit(1);
    }

    /**
     * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    /**
     * @see MqttCallback#messageArrived(String, MqttMessage)
     */
    public void messageArrived(String topic, MqttMessage message) throws MqttException {
	
        //obtengo el timestamp en el que llega el mensaje
        long timestamp = System.currentTimeMillis();	
        
        String contenido= new String(message.getPayload());
        
        //Calculo el retardo en milisegundos
        long diff = Math.abs(timestamp - Long.parseLong(contenido));
        System.out.println("Reatardo: " + String.valueOf(diff) + " ms");

            try{
                pw.println(String.valueOf(diff));
            }catch(Exception e){
                System.out.println("ERROR AL ESCRIBIR EN EL FICHERO");
            }  
    }

    public static void main(String[] args) throws MqttException, URISyntaxException {
        if(args.length != 0){
            qos = Integer.parseInt(args[1]);
            Subscriber s = new Subscriber(args[0]);
        }else{
            System.out.println("ERROR: falta el nombre del fichero de resultados");
            System.exit(0);
        }
	Runtime.getRuntime().addShutdownHook(new Thread() {
           public void run() {
               try {
		    fichero.close();
                   Thread.sleep(200);
                   System.out.println("Shouting down ...");
                   //some cleaning up code...

               } catch (InterruptedException | IOException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }
           }
       });
    }
}

