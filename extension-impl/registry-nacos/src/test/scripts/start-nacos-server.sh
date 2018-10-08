#!/bin/bash

echo "Downloading Nacos 0.2.1"
wget https://github.com/alibaba/nacos/releases/download/0.2.1/nacos-server-0.2.1.zip -O nacos-server-0.2.1.zip
unzip -qo nacos-server-0.2.1.zip

bash $PWD/nacos/bin/startup.sh -m standalone
sleep 10