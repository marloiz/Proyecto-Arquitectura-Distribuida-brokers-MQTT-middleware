#!/bin/bash

cd proxy-core
mvn compile
mvn install
mvn install:install-file -Dfile=target/proxy-core-1.0-SNAPSHOT.jar -DgroupId=com.proyecto -DartifactId=proxy-core -Dversion=1.0-SNAPSHOT -Dpackagin=jar
cd ..
#echo "Cambiamos de carpeta"


cd proxy
mvn compile
mvn install
mvn exec:java -Dexec.mainClass="com.proyecto.demo.App" -Dexec.args=”false”
