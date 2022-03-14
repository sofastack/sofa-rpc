#!/bin/bash

readonly shutdown_script="$PWD/nacos/bin/shutdown.sh"

if [ -f "$shutdown_script" ]; then
    bash "$shutdown_script"
fi
