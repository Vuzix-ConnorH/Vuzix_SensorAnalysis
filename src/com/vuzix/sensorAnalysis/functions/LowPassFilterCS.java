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
 * An instance of a Spherical Coordinate System that uses Android's
 *  Gravity / Magnetic Sensor to determine acceleration in X, Y, and Z values.
 *   
 * @author Connor Hack <connor_hack@vuzix.com>
 */
public class LowPassFilterCS extends SphericalCS {

	public LowPassFilterCS() {
		super();
	}
	
	@Override
	public String toString() {
		return "LowPassFilterCS" ;
	}
	
	/**
	 * Function to apply a Low Pass Filter on a data-set:
	 *  Allows low-frequency signals to pass and will reduce the amplitude
	 *  of high frequency signals
	 * 
	 * @param input - the data-set to modify
	 * @param output - the data-set to store the filtered values
	 * @return output
	 */
	public float[] filter( float[] input, float[] output ) {
		
		// Return if the output has not been initialized
	    if ( output == null ) {
	    	return input;     
	    }
	    
	    // Apply a low pass filter to the input
	    for ( int i=0; i<input.length; i++ ) {
	        output[i] = output[i] + 0.25f * (input[i] - output[i]);
	    }
	    
	    return output;
	}
	
	///////////////////////////////////////////////////////
	//   Below functions are for implementing Parcelable //
	///////////////////////////////////////////////////////

	private LowPassFilterCS(Parcel in) {
		super.x = in.readFloat();
		super.y = in.readFloat();
		super.z = in.readFloat();
		in.readFloatArray(super.origin);
	}
	
	public static Parcelable.Creator<LowPassFilterCS> CREATOR = new Parcelable.Creator<LowPassFilterCS>() {
		@Override
		public LowPassFilterCS createFromParcel(Parcel source) {
			// TODO Auto-generated method stub
			return new LowPassFilterCS(source);
		}
		@Override
		public LowPassFilterCS[] newArray(int size) {
			// TODO Auto-generated method stub
			return new LowPassFilterCS[size];
		}
	};	
}
