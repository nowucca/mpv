#!/bin/bash
cd com.nowucca.mpv.util
mvn clean install || exit 1
cd ../com.nowucca.mpv.core
mvn clean install || exit 1