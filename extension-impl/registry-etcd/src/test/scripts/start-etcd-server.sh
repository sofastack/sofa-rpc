#!/bin/bash

echo "Downloading etcd v3.3.10"
wget https://github.com/etcd-io/etcd/releases/download/v3.3.10/etcd-v3.3.10-linux-amd64.tar.gz
tar -zxvf  etcd-v3.3.10-linux-amd64.tar.gz

cd $PWD/etcd-v3.3.10-linux-amd64

nohup ./etcd &
echo $! > ../etcd.pid
sleep 5