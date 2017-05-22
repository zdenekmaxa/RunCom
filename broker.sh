#!/bin/sh

# Message Broker start script

# TDAQ release should be set up in order to have access to
# the default TDAQ Java runtime environment under $TDAQ_JAVA_HOME


BROKER_HOME=`dirname $0`
echo "Running the Message Broker from: $BROKER_HOME"
cd $BROKER_HOME
echo "Changing current directory there: $PWD"


# should be pointing to the correct production DAQ/HLT release
TDAQ_RELEASE_SETUP="/det/tdaq/scripts/setup_TDAQ.sh"


if [ "x$TDAQ_JAVA_HOME" == "x" ]
then
    echo "TDAQ release does not seem to be set up."
    echo "RunCom needs access to Java via TDAQ_JAVA_HOME env variable."
    echo "Setting up TDAQ release: $TDAQ_RELEASE_SETUP"
    if [ -r $TDAQ_RELEASE_SETUP ]
    then
        source $TDAQ_RELEASE_SETUP
    else
        echo "$TDAQ_RELEASE_SETUP does not exist."
        echo "Try setting up TDAQ release yourself, exit."
        exit 1
    fi
fi


echo "Java is available under: $TDAQ_JAVA_HOME"


# running the Message Broker
exec $TDAQ_JAVA_HOME/bin/java -jar $BROKER_HOME/broker.jar $*

