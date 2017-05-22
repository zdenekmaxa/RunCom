#!/usr/bin/env python

# Python broker for RunCom Broker

# author Zdenek Maxa <zdenek.maxa -- -- help.ucl.ac.uk>

# Could be used as base for dynamic solution when status overview given on 
# the ATLAS operation webpages is not static HTML output of the broker (as it
# is now 2009-03-17), but webserver executes this Python script which
# queries the RunCom broker (active-mq / JMS) and retrieves the status data.
# -> would however require implementation changes in RunCom broker - messages
# compatibilty between Java and Python ... 
# Communication with RunCom broker works

# Using pyactivemq library
# pyactivemq is a Python module for communicating with the ActiveMQ message
# broker which implements the Java Message Service specification.
# pyactivemq depends on: libboost, libcppunit, uuid and other libs and
# C++ implementation activemq-cpp

# Example corresponds to StartupSynchronizer.java class which each instance of
# RunCom executes upon its start-up to get up-to-date data fro the broker.
# Also based on DurableSubscriberExample.py example from pyactivemq

# If run on a webserver, the easiest would be to parse 'last' XML 
# persistent files *.last.rcpd.xml and present the information - will then
# get all necessary information.


import sys

try:
    import pyactivemq
except ImportError, ex:
    print "Cannot import necessary Python modules, reason: %s" % ex
    sys.exit(1)



class Synchronizer(object):

    UPDATE_QUERY_SUBJECT = "RunComMessageUpdateQuerySubject"
    UPDATE_RESPONSE_SUBJECT = "RunComMessageUpdateResponseSubject"
    RESPONSE_WAITING_TIMEOUT = 6000 # 6 seconds


    def __init__(self, connectionFactory):
        print "Synchronizer initialisation ..."
        self.connection = connectionFactory.createConnection()
        self.connection.start()
        print "Connection started."

        # create two sessions
        mode = pyactivemq.AcknowledgeMode.AUTO_ACKNOWLEDGE
        self.querySession = self.connection.createSession(mode)
        self.responseSession = self.connection.createSession(mode)
        print "Sessions created."

        # two destinations, one to which request is send via the other
        # response is received, queue - one-to-one delivery mode
        self.queryDest = \
            self.querySession.createQueue(Synchronizer.UPDATE_QUERY_SUBJECT)
        self.responseDest = \
            self.responseSession.createQueue( \
            Synchronizer.UPDATE_RESPONSE_SUBJECT)
        print "Destinations created."

        # create the producer
        self.producer = self.querySession.createProducer(self.queryDest)
        self.producer.deliveryMode = pyactivemq.DeliveryMode.PERSISTENT

        # create the consumer
        self.consumer = self.responseSession.createConsumer(self.responseDest)
        print "Message producer and consumer created."

        print "Message listener fully initialised."


    def stop(self):
        print "Closing connection ..."
        self.connection.close()
        print "Listener closed."


    def synchronize(self):
        """Send empty message as request."""

        print "Synchronize request ..."
        
        msg = self.querySession.createMessage()
        msg.replyTo = self.responseDest # important
        # self.queryDest does not seem necessary (1st arg)
        self.producer.send(msg)

        # synchronously receive message
        reply = self.consumer.receive(Synchronizer.RESPONSE_WAITING_TIMEOUT)

        # as a response to this request, RunCom broker actually sends three
        # responses - ArrayList<DeskDataMesage>, ArrayList<TextMessage> 
        # and the last WhiteBoardMessage, this script is only interested in
        # the first one
        
        # pyactivemq knows only about Message (and inherited BytesMessage,
        # MapMessage, TextMessage), without changes in Java RunCom broker it
        # just receives Message and is difficult to get the content of the
        # response ... the response is java.util.ArrayList<DeskDataMessage>,
        # with plain datatypes would be easy and possible ...

        print "Received: ", reply
        # print dir(reply)

        print "Synchronization finished."



def main():

    print "broker machine: the first and the only command line argument"
    brokerName = "localhost"
    if(len(sys.argv) > 1):
        brokerName = sys.argv[1]
    url = "".join(["tcp://", brokerName, ":", "61616"])
    
    print "trying to connect to broker: %s" % url

    factory = pyactivemq.ActiveMQConnectionFactory(url)

    # exceptions handling to be done ...
    listener = Synchronizer(factory)
    listener.synchronize()

    listener.stop()


if __name__ == "__main__":
    main()
