package com.proyecto;

import java.lang.System;
import java.lang.Thread;
import java.lang.InterruptedException;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

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
 * Hello world!
 *
 */
public class Publisher 
{

    private static int qos = 0;
    private String topic = "timestamp";
    private MqttClient client;

    public Publisher() throws FileNotFoundException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException, UnrecoverableKeyException{

        try{
            String host = String.format("ssl://%s:%d", "IP", "puerto (int)"); //poner IP y puerto del servidor

            String clientId = "MQTT-Java-Example-PUB";

            MqttConnectOptions conOpt = new MqttConnectOptions();
            conOpt.setCleanSession(true);


            // Establezco la configuracion SSL
            SSLSocketFactory ssf = configureSSLSocketFactory();
            conOpt.setSocketFactory(ssf);

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

    public static void main( String[] args ) throws FileNotFoundException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException,UnrecoverableKeyException
    {
        if(args.length != 0){
                qos = Integer.parseInt(args[0]);
        }else{                                                                                                                                                                     System.out.println("ERROR falta QoS");
            System.exit(0);
        }
        Publisher pub = new Publisher();

    }

}
