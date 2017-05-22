#!/bin/sh

# RunCom application start script

# TDAQ release should be set up in order to have access to
# the default TDAQ Java runtime environment under $TDAQ_JAVA_HOME

# RunCom interacts with the CheckList application and this RunCom start
# script relies on existence of (reference to checklist.jar is in the
# runcom.jar MANIFEST file)
# "../CheckList/checklist.jar"


RUNCOM_HOME=`dirname $0`
echo "Running the RunCom application from: $RUNCOM_HOME"
cd $RUNCOM_HOME
echo "Changing current directory there: $PWD"

# should be pointing to the correct production DAQ/HLT release
TDAQ_RELEASE_SETUP="/det/tdaq/scripts/setup_TDAQ.sh"

# checklist.jar must exist
CHECKLIST="../CheckList/checklist.jar"


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


if [ -r $CHECKLIST ]
then
    echo "CheckList appears to exist at: $CHECKLIST"
else
    echo "CheckList application $CHECKLIST not found."
    echo "RunCom depends on CheckList, install it / check it out first"
    echo "so that $CHECKLIST is available."
    exit 1
fi



# need to determine 'control' option for RunCom, that is the name of the 
# desk RunCom controls, it is translated from the actual username on the
# machine
username=`whoami`
case $username in
crsl)
    desknameparam="--control Shift Leader --supervisor"
    ;;
slimos)
    desknameparam="--control T.I. and SLIMOS"
    ;;
crrc)
    desknameparam="--control Run Control"
    ;;
crmon)
    desknameparam="--control DQ Monitor"
    ;;
crlum)
    desknameparam="--control FD-Lumi"
    ;;
crmuon)
    desknameparam="--control MDT"
    ;;
crlar)
    desknameparam="--control LAr"
    ;;
crtil)
    desknameparam="--control Tile"
    ;;
crtrt)
    desknameparam="--control TRT"
    ;;
crsct)
    desknameparam="--control SCT"
    ;;
crpixel)
    desknameparam="--control Pixel"
    ;;
crtrg)
    desknameparam="--control Trigger"
    ;;
crlvl1)
    desknameparam="--control LVL1"
    ;;
crdaq)
    desknameparam="--control DAQ-HLT"
    ;;
crdcs)
    desknameparam="--control DCS"
    ;;
*)
    echo "user $username doesn't have a desk assigned ... "
    desknameparam=""
esac


# running the RunCom application
exec $TDAQ_JAVA_HOME/bin/java -jar $RUNCOM_HOME/runcom.jar $desknameparam $*
