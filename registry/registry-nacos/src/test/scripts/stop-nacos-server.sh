#!/bin/bash

if [ -f "$PWD/nacos/bin/shutdown.sh" ]
then
    bash $PWD/nacos/bin/shutdown.sh
fi
