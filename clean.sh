#!/bin/bash
cd com.nowucca.mpv.util
mvn clean  || exit 1
cd ../com.nowucca.mpv.core
mvn clean  || exit 1