#!/bin/bash
nohup java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9004 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar RoomProcessor-0.0.1-SNAPSHOT.jar 1 >start_roomProcessor_1.log 2>&1 &
