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
 * An instance of a Coordinate System that uses Android's Gravity / Magnetic
 *  Sensor to determine acceleration in X, Y, and Z values.
 *   
 * @author Connor Hack <connor_hack@vuzix.com>
 */
public class SphericalCS extends CoordinateSystem {
	
	protected static float valueX ;
	protected static float valueY ;
	protected static float valueZ ;
	
	// Kept for debugging purposes
	protected static float oppInclination ;
	protected static float oppAzimuth ;
	
	///////////////////////////////////////////////////////////////////////////
	
	public SphericalCS() {
		super();
		valueX = 0 ;
		valueY = 0 ;
		valueZ = 0 ;
		oppInclination = 0 ;
		oppAzimuth = 0 ;
	}
	
	/**
	 * Setter function to set the current magnetic field value
	 * @param input - Magnetic field sensor value
	 */
	public void setValueX(float input[]) {
		valueX = (float) Math.sqrt( 	
					Math.pow(input[0], 2) + 
					Math.pow(input[1], 2) + 
					Math.pow(input[2], 2) ) ;
	}

	
	/**
	 * Setter function to set the current magnetic field value
	 * @param input - Magnetic field sensor value
	 */
	public void setValueY(float input) {
		valueY = input ;
	}
	
	/**
	 * Setter function to set the current magnetic field value and also
	 *  to set the current Quadrant based on the new value
	 * @param input - Magnetic field sensor value
	 */
	public void setValueZ(float input) {
		valueZ = input ;
		
	}
	
	
	@Override
	public void setVirtualOrigin(float[] coordinates) {
		super.setVirtualOrigin(coordinates);
		
		// Set the opposite value for the inclination :
		if (super.origin[3] >= 0) {
			oppInclination = super.origin[3] - 180 ;
		} else {
			oppInclination = super.origin[3] + 180 ;
		}
		
		// Set the opposite value for the azimuth :
		if (super.origin[4] >= 0) {
			oppAzimuth = super.origin[4] - 180 ;
		} else {
			oppAzimuth = super.origin[4] + 180 ;
		}
	}

	@Override
	public float getValueX() {
		// This represents the radial coordinate/radial distance
		
		return valueX;
	}
	
	@Override
	public float getValueY() {
		// Represents the inclination/polar angle (Roll)

		float returnValue ;
		
		// Apply the sign change for the appropriate circumstance
		if (super.z > 0) { 
			// Occurs when the value is below the origin's inclination
			returnValue = -valueY - super.origin[3] ;
		} else {
			// Occurs when the value is above the origin's inclination
			returnValue = valueY - super.origin[3] ;
		}
		
		/*
		 *  These two conditions are met when valueZ (the current raw azimuth
		 *  value) is in between the opposite value of the azimuth and the raw
		 *  azimuth value of 180.
		 */
		if (returnValue < -180) {
			// Happens when the inclination is positive
			returnValue += 360 ;
		} else if (returnValue > 180) {
			// Happens when the inclination is negative
			returnValue -= 360 ;
		}
		
		return returnValue ;
	}

	@Override
	public float getValueZ() {
		// Represents the azimuth/azimuthal angle (Yaw)
		
		float returnValue = (valueZ * RAD_TO_DEG) - super.origin[4] ;
		
		/* These two conditions are met when valueZ (the current raw azimuth
		 *  value) is in between the opposite value of the azimuth and the raw
		 *  azimuth value of 180.
		 */
		if (returnValue < -180) {
			// Happens when the azimuth origin is positive.
			returnValue += 360 ;
		} else if (returnValue > 180) {
			// Happens when the azimuth origin is negative.
			returnValue -= 360 ;
		}
		
		return returnValue ;
	}
	
	@Override
	public String getAccUnits(String input) {
		// Override the units for acceleration since the sensors used are
		//  different to that of the super class
		if (input.equalsIgnoreCase("x")) {
			return super.getAccUnits("x");
		} else if (input.equalsIgnoreCase("y")) {
			// Return a degrees symbol
			return String.valueOf((char) 0x00B0) ;
		} else if (input.equalsIgnoreCase("z")) {
			// Return a degrees symbol
			return String.valueOf((char) 0x00B0) ;
		} else {
			return super.getAccUnits("");
		}
	}
	
	@Override
	public String getDesc(String input) {
		// Override the units for acceleration since the sensors used are
		//  different to that of the super class
		if (input.equalsIgnoreCase("x")) {
			return "Magnitude:" ;
		} else if(input.equalsIgnoreCase("y")) {
			return "Inclination:";
		} else if(input.equalsIgnoreCase("z")) {
			return "Azimuth:" ;
		} else {
			return "" ;
		}
	}
	
	@Override
	public String toString() {
		return "SphericalCS" ;
	}
	
	// To find the Pitch, use this formula (y and z values from accelerometer): 
	// 		Pitch = RAD_TO_DEG * Math.atan( super.y / super.z )
	
	///////////////////////////////////////////////////////
	//   Below functions are for implementing Parcelable //
	///////////////////////////////////////////////////////

	private SphericalCS(Parcel in) {
		super.x = in.readFloat();
		super.y = in.readFloat();
		super.z = in.readFloat();
		in.readFloatArray(super.origin);
	}
	
	public static Parcelable.Creator<SphericalCS> CREATOR = new Parcelable.Creator<SphericalCS>() {
		@Override
		public SphericalCS createFromParcel(Parcel source) {
			return new SphericalCS(source);
		}
		@Override
		public SphericalCS[] newArray(int size) {
			return new SphericalCS[size];
		}
	};	
}
