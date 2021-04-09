#!/bin/sh

if [ "$1" ]; then
  version="$1"
else
  version="2.0"
fi

scripts/peer.sh "$version" 2 rmithing2 224.0.0.1 8081 224.0.0.2 8082 224.0.0.3 8083
