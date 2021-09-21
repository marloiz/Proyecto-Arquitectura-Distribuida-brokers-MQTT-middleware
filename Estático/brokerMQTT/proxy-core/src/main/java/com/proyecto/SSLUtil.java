/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.proyecto;

import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author thomas
 */
public class SSLUtil {

    private static final String STORETYPE = "JKS";
    private final String keyStorePassword;
    private final String keyStoreLocation;
    private String tlsVerison = "TLSv1.2";
    private String keyManagerInstance = "SunX509";

    public SSLUtil(String keyStorePassword, String keyStoreLocation) {
        this(keyStorePassword, keyStoreLocation, "", "");
    }

    public SSLUtil(String keyStorePassword, String keyStoreLocation, String tlsVersion, String keyManagerInstance) {
        this.keyStorePassword = keyStorePassword;
        this.keyStoreLocation = keyStoreLocation;
        if (tlsVersion.length() > 0) {
            this.tlsVerison = tlsVersion;
        }
        if (keyManagerInstance.length() > 0) {
            this.keyManagerInstance = keyManagerInstance;
        }
    }

    public final SSLContext getSSLContext() {
        try {
            KeyStore ks = KeyStore.getInstance(STORETYPE);
            InputStream in = getClass().getClassLoader().getResourceAsStream(keyStoreLocation);
            ks.load(in, keyStorePassword.toCharArray());
            in.close();

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerInstance);

            kmf.init(ks, keyStorePassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(keyManagerInstance);

            tmf.init(ks);

            SSLContext sslContext = null;
            sslContext = SSLContext.getInstance(tlsVerison);

            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            // sslContext.init( null, null, null ); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates

            return sslContext;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

}
