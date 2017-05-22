#!/usr/bin/env python

# author Zdenek Maxa <zdenek.maxa -- -- hep.ucl.ac.uk>

# Drawing statistics based on RunCom Broker XML persistent serialised data.
#
# Uses scipy library.
# Produces a bar chart overview showing how much each system, whose XML
# persistent data was read in, spent in each state (status).

# If HTML output is to be produced, easy to create bar charts via HTML
# table (see HTML notes)



import sys
import os
import string
import re
import datetime
import xml.etree.ElementTree as et


try:
    import util
    import numpy.numarray
    import pylab
    import matplotlib
except ImportError, ex:
    print "Cannot import necessary Python modules, reason: %s" % ex
    sys.exit(1)



class StatesInfo(object):
    """
    Defines states and corresponding properties of states, e.g. colours.
    State and status terms are used interchangeably.
    
    """

    def __init__(self):
        self.states =  ["signed off", "signed in", "error", "ready"]
        self.colors = dict(zip(self.states, ["grey", "blue", "red", "green"]))
        # self.icons = [] icon gif files if necessary in the legend


    def getColor(self, state):
        if self.isValidState(state):
            return self.colors[state]
        else:
            raise ValueError("Unknown status (state) name: '%s'" % state)


    def isValidState(self, state):
        if state in self.states:
            return True
        else:
            return False


    def getStates(self):
        return self.states



class DeskData(object):
    
    # statesInfo is sort of check object if values inserted into DeskData
    # are really valid - doesn't change, the same for all DeskData instances,
    # thus static
    statesInfo = StatesInfo()

    def __init__(self):
        self.name = "" # desk name

        # key: state (status) name, value: list of time stamps for each status
        # actual duration is calculated as 2nd - 1st + 4th - 3rd time, ... 
        # if there is an odd-th timestamp, it is discarded
        # 0 is returned if having an empty list (no updates for a given state)
        s = DeskData.statesInfo.getStates()
        self.duration = dict(zip(s, [list() for i in range(len(s))]))


    def setName(self, name):
        self.name = name


    def addTimeStampForState(self, state, stamp):
        if DeskData.statesInfo.isValidState(state):
            self.duration[state].append(stamp)
        else:
            raise ValueError("Unknown status (state) name: '%s'" % state)


    def getName(self):
        return self.name


    def getDateTime(self, strTime):
        """
        Converts string representation of time to datetime object.
        strTime is format '2009-03-03 15:37:52.903 CET' (from Java),
        ignores the timezone bit.
        
        """
        # solution datetime.datetime.strptime(string, format) does not work
        # since milliseconds does not have corresponding format character (!)

        # another solution:
        timesep = re.compile("[-\ :.]") # separators in the timestring
        l = timesep.split(strTime)
        # -1 : without the timezone
        d = datetime.datetime(*[int(i) for i in l[:-1]])
        return d


    def _calculateDuration(self, past, future):
        """
        Returns duration difference between input times in minutes.
        
        """
        dtPast = self.getDateTime(past)
        dtFuture = self.getDateTime(future)

        delta = dtFuture - dtPast

        # r is duration in minutes
        r = delta.seconds / 60

        print "duration: '%s' - '%s' = '%s' (%s min.)" % (dtFuture, dtPast,
                delta, r)

        return r


    def getDurationForState(self, state):
        if DeskData.statesInfo.isValidState(state):
            r = 0
            d = self.duration[state] # list

            # iterate by two, for odd number of items - the last one item
            # would be ignored (having - 1) since not having the end time
            # for which the state lasted
            for i in range(0, len(d) - 1, 2):
                r += self._calculateDuration(d[i], d[i + 1])
            print "\ttotal duration: %s min." % r
            return r
        else:
            raise ValueError("Unknown status (state) name: '%s'" % state)



def setPlot():
    pylab.xlabel("systems / desks")
    pylab.ylabel("duration [minutes]")
    pylab.title("RunCom system statistics")
    pylab.grid(True)

    pylab.setp(pylab.gca().get_xticklabels(), rotation=90,
                horizontalalignment='left')

    # wspace, hspace does not seem to have any effect
    pylab.subplots_adjust(left = 0.1, right = 0.95, bottom = 0.25,
                          top = 0.85, wspace = 0.45, hspace = 0.7)

    
def drawData(desks):

    width = 0.05 # width of bar

    legend = [] # states and colours explanation
    si = StatesInfo()
    states = si.getStates()
    x = 1 # start x position (first bar x coordinate)
    xPosLabels = [] # x coordinates of labels - names of desks
    xLabels = [] # labels on x-axis - names of desks
    # iterate over all desks
    for d in range(len(desks)):
        xPosLabels.append(x)
        xLabels.append(desks[d].getName())
        # iterate over states for a desks
        maxDuration = 0 # maximum duration (to adjust y axis on the plot)
        for s in range(len(states)):
            print "\nDesk '%s' state '%s' calculation" % (desks[d].getName(),
                    states[s])
            dur = desks[d].getDurationForState(states[s])
            b = pylab.bar(left = x, height = dur, width = width,
                          color = si.getColor(states[s]))
            x += width
            if d == 0:
                # is the first system, get data for legend
                legend.append(b)
            # save maximum duration (to adjust y axis on the plot)
            if dur > maxDuration:
                maxDuration = dur
        x += 2 * width # space between group of bars belonging to a system
    

    # adjusting the size of the plot
    # 1) via pylab.rcParams figure.figsize, figure.subplot.top, etc - does not
    #    work
    # 2) pylab.figure.xlim, etc - does not work
    # 3) works - adjust the y axis range
    ymax = maxDuration + (maxDuration * 0.1) # increase y axis range by 10%
    pylab.ylim(ymin = 0, ymax = ymax)

    pylab.rcParams.update({'legend.fontsize':8})

    pylab.xticks(xPosLabels, xLabels)  # draw x axis labes - names of desks
    # pylab.legend() loc='best' loc='upper right'
    pylab.legend(legend, states, shadow=True, loc='upper right')
    
    pylab.show()

    # saves plot into a png file
    #pylab.savefig('simple_plot')



def convertDeskXMLFileToDeskData(fileName):
    """
    Converts content of the RunCom persistent XML file fileName containing
    data for a particular desk into DeskData object which holds information
    about desk name (derived from fileName up to first '.' occurence) and
    duration the desk spent in all states (desk statuses).
    
    """
    r = DeskData()

    print "processing file '%s'" % fileName

    parser = et.XMLParser()
    # need to provide root elements, otherwise tree = et.parse(fileName)
    # fails at second tag DeskDataMessage (expects the first DeskDataMessage
    # be the root element)
    parser.feed("<root>")
    data = open(fileName, 'r').read()
    parser.feed(data)
    parser.feed("</root>")
    root = parser.close()
    
    print "%s instances found." % len(root.getchildren())

    prevState = "" # remember this to adjust duration
    deskName = ""
    for c in root.getchildren():
        status = ""
        dateTime = ""
        for i in c:
            # iterate over particular tags within the DeskDataMessage
            # (attributes would be available via dictionary)
            if i.tag == "deskName":
                deskName = i.text # actually needed only once
            elif i.tag == "timeOfMessage":
                dateTime = i.text
            elif i.tag == "currentStatus":
                status = i.text

        if prevState != "":
            # must be second <DeskDataMessage> tag and onwards
            # safe this time stamp as end time for the previous status
            r.addTimeStampForState(prevState, dateTime)
        r.addTimeStampForState(status, dateTime)

        prevState = status # important to safe previous status

    r.setName(deskName)

    return r


def main():
    fileMask = "*.all.rcpd.xml"
    print "RunCom - generating desks/states duration statistics"

    msg = ("%s requires -d <directory> arguments to read RunCom "
            "persistent XML data from, exit." % sys.argv[0])
    srcDir = ""
    if len(sys.argv) != 3:
        print msg
        sys.exit(1)
    elif sys.argv[1] == "-d" or sys.argv[1] == "--directory":
        srcDir = sys.argv[2]
    else:
        print msg
        sys.exit(1)

    print "Reading RunCom XML persistent data from '%s'" % srcDir


    try:
        filesList = util.getListOfFiles(srcDir, fileMask = fileMask)
    except Exception, ex:
        print ("Error while getting list of files from '%s', reason: %s" %
            (srcDir, ex))
        sys.exit(1)


    desks = [] # list of DeskData objects
    for fileItem in filesList:
        try:
            d = convertDeskXMLFileToDeskData(fileItem)
            desks.append(d)
        except Exception, ex:
            print ("erorr occured while processing file '%s'. reason: '%s' "
                   "ignoring this file." % (fileItem, ex))  

    setPlot()
    drawData(desks)


if __name__ == "__main__":
    main()
