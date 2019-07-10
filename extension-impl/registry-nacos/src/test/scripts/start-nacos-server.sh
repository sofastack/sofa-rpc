#!/bin/bash
if [ ! -f nacos-server-0.5.0.zip ]; then
    echo "Downloading Nacos 0.6.0"
    wget https://github.com/alibaba/nacos/releases/download/0.6.0/nacos-server-0.6.0.zip -O nacos-server-0.6.0.zip
    unzip -qo nacos-server-0.6.0.zip
fi

echo "Starting nacos server..."
nohup bash $PWD/nacos/bin/startup.sh -m standalone >> $PWD/nacos/start.out 2>&1 &
sleep 10