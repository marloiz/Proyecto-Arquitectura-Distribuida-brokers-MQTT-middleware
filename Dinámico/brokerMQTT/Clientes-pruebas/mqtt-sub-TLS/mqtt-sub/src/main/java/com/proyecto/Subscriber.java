package com.proyecto;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.URI;
import java.net.URISyntaxException;

import java.io.*;
import java.io.InputStream;
import java.io.FileInputStream;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.security.UnrecoverableKeyException;




/**
 * A sample application that demonstrates how to use the Paho MQTT v3.1 Client blocking API.
 */
public class Subscriber implements MqttCallback {

    private static int qos = 0;
    private String topic = "timestamp";
    private MqttClient client;
    private static FileWriter fichero;
    private PrintWriter pw;


    public Subscriber(String nombreFichero) throws URISyntaxException, MqttException, FileNotFoundException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException, UnrecoverableKeyException {
       // this(new URI(uri));
	try{
	    fichero = new FileWriter("src/main/output/"+nombreFichero);
	    pw = new PrintWriter(fichero);
	}catch(Exception e){
	}
	iniciarSubscripcion();
    }

    public void iniciarSubscripcion() throws MqttException, FileNotFoundException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException, UnrecoverableKeyException  {

        String host = String.format("ssl://%s:%d", "192.168.1.6", 8883);

        String clientId = "MQTT-Java-Example";

        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(true);

        // Establezco la configuracion SSL
        SSLSocketFactory ssf = configureSSLSocketFactory();
        conOpt.setSocketFactory(ssf);

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
        }

    }



    public SSLSocketFactory configureSSLSocketFactory() throws FileNotFoundException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException, UnrecoverableKeyException  {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream jksInputStream = new FileInputStream("src/main/resources/config/clientkeystore.jks");
        ks.load(jksInputStream, "password".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "password".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sc = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = tmf.getTrustManagers();
        sc.init(kmf.getKeyManagers(), trustManagers, null);

        SSLSocketFactory ssf = sc.getSocketFactory();
        return ssf;
    }

    public static void main(String[] args) throws MqttException, URISyntaxException, FileNotFoundException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException,UnrecoverableKeyException {


        if(args.length !=0){
            qos = Integer.parseInt(args[1]);
            Subscriber sub = new Subscriber(args[0]);
        }else{
            System.out.println("ERROR: falta el nombre del fichero de resultados");
            System.exit(0);
        }


        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
            try{
                fichero.close();
                Thread.sleep(200);
            }catch(InterruptedException | IOException e){
                e.printStackTrace();
            }
            }
        });

    }
}

