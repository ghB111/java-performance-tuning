#!/usr/bin/env bash

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64/
javac -h . ./ru/nsu/fit/cpucheker/CPUInfo.java
g++ -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -shared -o libcpu.so CPUInfo.cpp

