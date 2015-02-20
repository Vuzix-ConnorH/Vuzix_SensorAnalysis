/*
 * Copyright (c) 2015, Vuzix Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

*  Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
    
*  Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
    
*  Neither the name of Vuzix Corporation nor the names of
   its contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.
    
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package com.vuzix.sensorAnalysis.functions;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * An abstract class to extend to the below coordinate systems:
 *  - Raw Coordinate System
 *  - Absolute Coordinate System from a zeroed position
 *  - Spherical Coordinate System based on the Physics representation
 *    (Based on Absolute Coordinate System)
 *  - Low Pass Filtered Coordinate System (Based on Absolute Coordinate System)
 *  - High Pass Filtered Coordinate System (Based on Absolute Coordinate System)
 * 
 * Reason being for an abstract class is that each coordinate system calculates its
 *  x, y, z coordinates uniquely.
 * 
 * For assistance understanding Parcelable, reference:
 *  http://stackoverflow.com/questions/2139134/how-to-send-an-object-
 *  from-one-android-activity-to-another-using-intents
 * 
 * @author Connor Hack <connor_hack@vuzix.com>
 *
 */
public abstract class CoordinateSystem implements Parcelable {
	
	// Approximately 57 degrees
	protected static final float RAD_TO_DEG = (float) (180 / Math.PI) ;
	
	// Original values that are to be kept for new calculations
	protected float x ;
	protected float y ;
	protected float z ;
	
	// Values to be used as initial values within equations
	protected float velocity[] ;
	protected float position[] ;
	
	// The virtual origin used to process this system's coordinates
	protected float[] origin ;
	
	/**
	 * Constructor
	 */
	public CoordinateSystem() {
		x = 0 ;
		y = 0 ;
		z = 0 ;
		origin = new float[5] ;
		velocity = new float[3] ;
		position = new float[3] ;
	}
	
	/**
	 * Setter function to (re)set the value of the virtual origin
	 * @param coordinates - new values for the origin 
	 * 		[0] = x-value from accelerometer
	 * 		[1] = y-value from accelerometer
	 * 		[2] = z-value from accelerometer
	 * 		[3] = inclination (degrees)
	 * 		[4] = azimuth (degrees)
	 */
	public void setVirtualOrigin(float[] coordinates) {
		if (coordinates.length != 5) {
			return ;
		}
		origin[0] = coordinates[0] ;
		origin[1] = coordinates[1] ;
		origin[2] = coordinates[2] ;
		origin[3] = coordinates[3] ;
		origin[4] = coordinates[4] ;
	}
	
	/**
	 * Setter function to input new values for the x,y,z coordinates
	 * These are initial values and not to be used except within their
	 *  respective 'get' functions.
	 * 
	 * Initially used to accommodate for the spherical coordinates usage
	 *  of all three values within the calculation of one value.
	 * @param coordinates - float array: { <x>, <y>, <z> }
	 */
	public void setValues(float[] coordinates) {
		x = coordinates[0] ;
		y = coordinates[1] ;
		z = coordinates[2] ;
	}
	
	/**
	 * Function to return the x value to the designated coordinate system
	 * @return - the new computed value
	 */
	public float getValueX() {
		
		return x;
	}
	
	/**
	 * Function to return the y value to the designated coordinate system
	 * @return - the new computed value
	 */
	public float getValueY() {
		
		return y;
	}
	
	/**
	 * Function to return the z value to the designated coordinate system
	 * @param input - the value of the original coordinate
	 * @return - the new computed value
	 */
	public float getValueZ() {
		
		return z;
	}
	
	/**
	 * Function to calculate the velocity from a given input
	 * @param input - an acceleration value
	 * @param time - a time value (ms)
	 * @param index - signifies if it is the x, y, or z value
	 * @return - a velocity value based on current acceleration
	 */
	public float getVelocity(float input, long time, int index) {
		return input ;
	}
	
	/**
	 * Function to calculate the position from a given input
	 * @param input - an acceleration value
	 * @param time - a time value (ms)
	 * @param index - signifies if it is the x, y, or z value
	 * @return - a position value based on current acceleration
	 */
	public float getPosition(float input, long time, int index) {
		return input ;
	}
	
	/**
	 * Returns a value indicating the units to grab
	 * @return input - a string: m/s^2, m/s, or m
	 */
	public String getAccUnits(String input) {
		if(	input.equalsIgnoreCase("x") ||
			input.equalsIgnoreCase("y") ||
			input.equalsIgnoreCase("z") ) {
				return "m/s" + (char) 0x00B2 ;
		} else {
				return null;
		}
		
	}
	
	/**
	 * Returns the description of the specified value
	 * @param input - a string describing which value to describe
	 * @return a string describing the input
	 */
	public String getDesc(String input) {
		if (input.equalsIgnoreCase("x")) {
			return "X:" ;
		} else if(input.equalsIgnoreCase("y")) {
			return "Y:";
		} else if(input.equalsIgnoreCase("z")) {
			return "Z:" ;
		} else {
			return "" ;
		}
	}
	
	/**
	 * Function to retrieve the current virtual origin
	 * @return
	 */
	public float[] getVirtualOrigin() {
		return origin ;
	}
	
	@Override
	public String toString() {
		return "CoordinateSystem" ;
	}
	

	///////////////////////////////////////////////////////
	//   Below functions are for implementing Parcelable //
	///////////////////////////////////////////////////////
	
	//@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// Write the object's data to the passed-in Parcel
	//@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeFloat(x);
		out.writeFloat(y);
		out.writeFloat(z);
		out.writeFloatArray(origin);
	}
	
	// Must implement this in subclasses
	private CoordinateSystem(Parcel in) {
		x = in.readFloat();
		y = in.readFloat();
		z = in.readFloat();
		origin = in.createFloatArray();
	}
}
