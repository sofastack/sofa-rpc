#!/bin/bash

nacos_version="2.0.3"

echo "Downloading Nacos ${nacos_version}"
wget https://github.com/alibaba/nacos/releases/download/${nacos_version}/nacos-server-${nacos_version}.zip -O nacos-server-${nacos_version}.zip
unzip -qo nacos-server-${nacos_version}.zip

nohup bash "$PWD"/nacos/bin/startup.sh -m standalone >> "$PWD"/nacos/start.out 2>&1 &
sleep 10
