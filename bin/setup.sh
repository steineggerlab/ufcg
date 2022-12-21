#!/usr/bin/env bash
SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
export PATH="$PATH:$SCRIPTPATH"
echo "export PATH=\"\$PATH:$SCRIPTPATH\"" >> ~/.bashrc
