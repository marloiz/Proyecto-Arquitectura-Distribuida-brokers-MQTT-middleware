#!/bin/bash


cd proxy
mvn exec:java -Dexec.mainClass="com.proyecto.demo.App" -Dexec.args=”false”
