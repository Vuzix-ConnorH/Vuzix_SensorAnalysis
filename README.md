Author:   Connor Hack

Project:  Sensor Analysis

Date:     2/17/15


###### Copyright (c) 2015, Vuzix Corporation


# CONTENTS OF THIS FILE

- - - -
  1. Description of Project
    1. Project Inception
    2. Functionality
    3. Usage
  2. Project Source Contents
  3. Known Issues
  4. Installation

- - - -

###  1. Description of Project

##### 1. Project Inception

This project was used to send along with a refurbished device to a 
potential partner of micro-location processing.  The project was sent with
its source code to show an example of a sample application on the M100.
     

2. Functionality

The project contains five different views for displaying information on 
the M100 sensors.  There exists the:
  
* Raw Coordinates view that displays raw accelerometer x, y, z values.
* Absolute Coordinates view that displays raw linear acceleration x, y,
   z values.
* Spherical Coordinates view that displays information from the M100's
   accelerometer, gravity, and magnetic sensor.  The x value is the 
   square root of the addition of x^2, y^2, and z^2 on the 
   accelerometer, the y and z value are respectively the inclination 
   and azimuth values found by obtaining orientation from the gravity 
   and magnetic sensor.
* Low Pass Filtered Coordinates that takes the Spherical Coordinates
   values and applies a low pass filter to its three values.
* High Pass Filtered Coordinates that takes the Spherical Coordinates 
   values and applies a high pass filter to its three values.

In addition to the five Coordinate Systems (CS), there is also a required
button to Reset the Virtual Origin.  This is needed for all Spherical CS
that require the gravity vector to be established.  The system averages 
all entries collected over a 3.5 second time period.  The number of 
entries collected is based on the established collection rate.

Within the action bar, there exists a spinner (drop-down menu) to select
the rate at which the sensors collect data.  The faster the rate, the
faster the sensors collect data.

3. Usage

This project was designed to run on a Vuzix M100.  It has not been tested 
on a smartphone device.  Due to differences in axes, the application will
most likely not function properly if run on a smartphone.

The project was developed on Eclipse IDE for Java Developers, Version:
Luna Service Release 1a (4.4.1). The Project Build Target is Android API
Level 15 (4.0.3) due to requirements of the Vuzix M100. It was compiled
with Java 1.6.

Upon initial start up of the application, the User will notice a Toast
message indicating the Sensor Rate being set to the Normal Rate. This
indicates that the sensors have been set to GAME mode. The higher the rate, 
the more data collected, but battery & CPU usage is increased as well.

In order to view Spherical Coordinates, it is requried to Reset the
Virtual Origin.  It is critical during setting of the virtual origin that
the user DOES NOT MOVE the device.  Extraneous values will be averaged
into the virtual origins values.  The virtual origin is made up of: x/y/z 
values from the accelerometer, an inclination value, and an azimuthal
value.

Once a Coordinate View is selected, the User will be able to view three
values (depending on the CS selected) and a CPU Usage stat that indicates
the percentage of the CPU the application is currently using. In addition,
the User is also able to change the collection rate of the sensors to
modify the CPU usage as well as the rate at which the display shows sensor
information.


- - - -  
2. Source Contents
-------------------------------------------------------------------------------
The Source folder (src) contains two packages:

- com.vuzix.sensorAnalysis.activities (referred to as Activities)
 + MainActivity.java
 + SensorActivity.java
- com.vuzix.sensorAnalysis.functions  (referred to as Functions)
 + AbsoluteCS.java
 + CoordinateSystem.java
 + HighPassFilterCS.java
 + LowPassFilterCS.java
 + RawCS.java
 + SphericalCS.java

The Activities package contains the two activities that run the project. The
MainActivity.java file is the initial activity that starts the application.
This houses functionality to start each Coordinate View as well as Setting
the Virtual Origin.  The second activity, SensorActivity.java, contains
functionality to run each Coordinate System.

The second package, Functions, contains the specific extensions of the 
abstract class CoordinateSystem.java.  CoordinateSystem.java establishes a 
base class to extend from so that SensorActivity.java can easily accomodate
for any instances of CoordinateSystem.java.  This provides a reduction of 
repeat code and more fluidity.  As each name indicates, the classes within
Functions are each a CoordinateSystem.
   
- - - -
3. Known Issues
-------------------------------------------------------------------------------
There exists a couple of known issues, or limitations, of the device.  If a
user moves the device around while the device is calibrating the virtual
origin, there will not be a reliable origin to use for the Spherical
Coordinate Systems.

The two action bars for the two different activites, MainActivity.java and 
SensorActivity.java are not the same.  If a User modifies the the collection
rate in one view, it will not modify the rate in another view.

If a user has significantly tilted his/her head to the left or right while
trying to read the inclination value, the inclination value will jump.  This
is a known issue with an unknown solution.

- - - -
4. Installation
-------------------------------------------------------------------------------
- To install the application on a device, simply install the 
  com.vuzix.sensorAnalysis.apk file included in this zip file.

   - E.g. Within Linux, and with the Vuzix M100 device connected to the PC,
          use the command:
     $ adb install com.vuzix.sensorAnalysis.apk

- To install the project into Eclipse with ADK plugin, simply follow:

   > File > Import... > General > Existing Projects into Workspace

   and input the location of the Vuzix_SensorAnalysis.zip file in the space
   alloted for the archive file.

 - To install the project into Android Studio, simply follow:

   > File > Import Project... 

   and input the location of the UNZIPPED Vuzix_SensorAnlysis.zip file. From 
   here, Clean the project and then Make the project.
     
     
###### Copyright (c) 2015, Vuzix Corporation
