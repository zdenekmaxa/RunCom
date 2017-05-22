## CERN/ATLAS experiment RunComm application

RunCom - Run Time Communicator.

ATLAS control room expert operator application allowing shifters communicate
among experiments subsystem's control desks, indicating their status and
transmitting messages.

Shift Leader's RunCom instance offers WhiteBoard and supervising features over
other instances.


### Instalation

The instructions are applicable for the CheckList application as well.

Deploying RunCom/CheckList at the ATLAS Point 1 network:

- CVS contain JAR files (Java executables) complied with Java version
    1.6 (this version is currently - 2009-02-16 - used in TDAQ as well)

- Getting packages from CVS, from
    `common/RunCom` and `common/CheckList`

- Installation account: `ssh -l acrtoolswinstaller pc-tdq-cfs-03`

- Copy sw into `/sw/ACR` under `CheckList` and `RunCom`

- Synchronize (machines on P1 network):
    `sudo -u atdadmin /daq_area/tools/sync/remote_sync.sh -x -t sw_ACR`

    more details [here](https://pcatdwww.cern.ch/FAQ/point1/index.php?action=artikel&cat=4&id=78&artlang=en)

- Restart the `RunCom Broker` - it is running as a service on LSF server,
    aliased as `pc-tdq-lfs-runcombroker`

    ```
    ssh pc-tdq-lfs-runcombroker
    sudo /sw/ACR/RunCom/RunCom_broker_service stop|start
    ```

- `/det/tdaq/ACR/XMLdata` directory which contains all CheckList XML data
    (content) files is accessible for everybody

