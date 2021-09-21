package com.proyecto;

import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

public class InforTimeOut {

	//public String ID; // ID del broker transmisor del mensaje
	public String port; // Puerto del broker transmisor del mensaje
	public ScheduledExecutorService scheduler;  
	//public Runnable runnable;


	public InforTimeOut (String _port, ScheduledExecutorService _scheduler) {
		port = _port;
		scheduler = _scheduler;
		//runnable =  _runnable;
	
	}

}
