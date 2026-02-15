#!/bin/sh
MAIN_CLASS=${MAIN_CLASS:-it.polimi.hyperh.apps.LocalApp}
echo "--- Starting Node: $NODE_ID ---"
echo "--- Instance: $NRP_INSTANCE ---"
java -cp /app/app.jar $MAIN_CLASS