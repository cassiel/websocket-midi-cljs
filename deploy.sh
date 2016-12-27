#!/bin/bash

# Deploy into server source tree (so that it can be committed and uploaded to Heroku).

SRC=./resources/public
DEST=../websocket-midi-server/static

for d in js jslib css; do
    scp -pr $SRC/$d $DEST/
done
