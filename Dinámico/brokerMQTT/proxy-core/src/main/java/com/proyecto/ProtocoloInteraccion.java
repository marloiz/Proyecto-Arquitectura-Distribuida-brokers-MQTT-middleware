package com.proyecto;

import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;

import java.util.concurrent.Semaphore;

public interface ProtocoloInteraccion {

	public Boolean iniciarComunicacion(InterceptPublishMessage pubMsg, String tipoAccion);

}
