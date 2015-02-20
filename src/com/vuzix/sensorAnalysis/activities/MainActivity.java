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

package com.vuzix.sensorAnalysis.activities;

import java.text.DecimalFormat;
import java.util.Hashtable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.vuzix.sensorAnalysis.R;
import com.vuzix.sensorAnalysis.functions.AbsoluteCS;
import com.vuzix.sensorAnalysis.functions.HighPassFilterCS;
import com.vuzix.sensorAnalysis.functions.LowPassFilterCS;
import com.vuzix.sensorAnalysis.functions.RawCS;
import com.vuzix.sensorAnalysis.functions.SphericalCS;

/**
 * This is the main activity that the application starts from.
 * 
 * Contains functionality to load each coordinate system as well as
 *  set the virtual origin.
 * 
 * Number of Entries (per 3.5 second collection period): 
 *  - Under rate of Normal, recommended value of ENTRIES is 25 (7.14 records per second)
 *  - Under rate of UI, recommended value of ENTRIES is 60 (17.14 records per second)
 *  - Under rate of Game, recommended value of ENTRIES is 225 (75.00 records per second)
 *  - Under rate of Fastest, recommended value of ENTRIES is 1000 (285.71 records per second)
 *  
 * @author Connor Hack <connor_hack@vuzix.com>
 */

public class MainActivity extends Activity implements SensorEventListener {
	
	private static final int DEGREES = 3 ;
	private static final int WAIT_TIME_ORIGIN = 3500 ; // Approx. 3.5 seconds
	// Approximately 57 degrees
	private static final float RAD_TO_DEG = (float) (180 / Math.PI) ;
	
	// Constants created for ease of use within code
	public static final int DELAY_NORMAL = SensorManager.SENSOR_DELAY_NORMAL;
	public static final int DELAY_UI = SensorManager.SENSOR_DELAY_UI;
	public static final int DELAY_GAME = SensorManager.SENSOR_DELAY_GAME;
	public static final int DELAY_FASTEST = SensorManager.SENSOR_DELAY_FASTEST;
	
	// A hashtable to access the number of entries to use based on the current 
	//  rate established by the spinner.
	@SuppressWarnings("serial")
	public static final Hashtable<Integer, Integer> RATES = new Hashtable<Integer, Integer>() {{
		put(DELAY_NORMAL, 25);
		put(DELAY_UI, 60);
		put(DELAY_GAME, 225);
		put(DELAY_FASTEST, 1000);
	}};
	
	private static int RATE = DELAY_GAME ;
	
	protected RawCS rawCoordinates ;
	protected AbsoluteCS absoluteCoordinates ;
	protected SphericalCS sphericalCoordinates ;
	protected LowPassFilterCS lpfCoordinates ;
	protected HighPassFilterCS hpfCoordinates ;
	
	private SensorManager mSensorManager ;
	private Sensor sensorAccelerometer, sensorMagnetic, sensorGravity;
	
	// Globals for collected virtual origin
	private float coordinates[][] ; // Measurements collected during origin test
	private int counter_origin, counter_orient ;
	private boolean doCollect = false ;
	
	// Globals to use for onSensorChanged (used for establishing polar/azimuthal angle)
	private float mGravity[] = new float[3] ;
	private float mMagnetic[] = new float[3] ;
	private float mInclination[] = new float[9] ;
	private float mRotation[] = new float[9] ;
	private float mValues[] = new float[3] ;
	
	private float northAzimuth, northInclination ;
	
	private float sign ;
	
	/////////////////////////////////////////////////////
	// Below methods are for the extension of Activity //
	/////////////////////////////////////////////////////
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initGlobals() ;
		
		setContentView(R.layout.activity_main);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setSensorRates(RATE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
	    // Inflate the menu items for use in the action bar
	    getMenuInflater().inflate(R.menu.menu_main, menu);
	    
	    // Find the menu id and create a corresponding adapter
	    MenuItem item = menu.findItem(R.id.menuRate);
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
	    		R.array.rates, R.layout.spinner_rates_header);
	    
	    // Specifies the layout to use when the list of choices appears
	    adapter.setDropDownViewResource(R.layout.spinner_rates_items);
	    
	    // Find the spinner and attach the adapter to it
	    Spinner spinner = (Spinner) item.getActionView().findViewById(R.id.spinner_rates);
	    spinner.setAdapter(adapter);
	    
	    // Store the context for the spinner's listener to create toast messages
	    final Context mContext = this.getApplicationContext();
	    
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				
				// Grab the item that was selected
                Object item = parent.getItemAtPosition(pos);
                
                if (item != null) {
                	// Create a default toast message if something unexpected occurs
                    Toast toast = Toast.makeText(mContext, "There was an unexpected error.", Toast.LENGTH_SHORT);
                    
                	if(item.toString().equals(mContext.getResources().getString(R.string.rate_normal))) {
                		// Set the sensor rate to the Normal mode
                		RATE = DELAY_NORMAL ;
                		toast.setText("Sensor rate set to Slowest Rate.");
                		
                	} else if(item.toString().equals(mContext.getResources().getString(R.string.rate_ui))) {
                		// Set the sensor rate to the UI mode
                		RATE = DELAY_UI ;
                		toast.setText("Sensor rate set to Slow Rate.");
                		
                	} else if(item.toString().equals(mContext.getResources().getString(R.string.rate_game))) {
                		// Set the sensor rate to the Game mode
                		RATE = DELAY_GAME ;
                		toast.setText("Sensor rate set to Normal Rate.");
                		
                	} else if(item.toString().equals(mContext.getResources().getString(R.string.rate_fastest))) {
                		// Set the sensor rate to the Fastest mode
                		RATE = DELAY_FASTEST ;
                		toast.setText("Sensor rate set to Fast Rate.");
                	}
                	
                	// Reset variables dependent on the sensor rate
                	setSensorRates(RATE);
                	coordinates = new float[RATES.get(RATE)][DEGREES] ;
                	toast.show();
                }
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		
		// Set the initial selection at the middle value
		spinner.setSelection((int) (Math.floor( (RATES.size()-1)/2) ));
		// Give initial focus to the spinner
		spinner.requestFocus();
	    
	    return super.onCreateOptionsMenu(menu);
	}
	
	////////////////////////////////////////////////
	// Below methods are specific to MainActivity //
	////////////////////////////////////////////////
	
	/**
	 * Helper function to initialize the global variables
	 */
	private void initGlobals() {
		
		// Initialize the coordinate systems
		rawCoordinates = new RawCS() ;
		absoluteCoordinates = new AbsoluteCS() ;
		sphericalCoordinates = new SphericalCS() ;
		lpfCoordinates = new LowPassFilterCS() ;
		hpfCoordinates = new HighPassFilterCS() ;
		
		// Initialize the sensor manager and accelerometer sensor
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE) ;
		sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ;
		sensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		// Warn the user via Log if specific sensors are not available
		if (sensorAccelerometer == null) {
			Log.w("M100Dev", "Device does not possess an accelerometer. "
					+ "This app will not function properly.");
		}
		if (sensorMagnetic == null) {
			Log.w("M100Dev", "Device does not possess a magnetic sensor. "
					+ "This app will not function properly.");
		}
		if (sensorGravity == null) {
			Log.w("M100Dev", "Device does not possess a gravity sensor. "
					+ "This app will not function properly.");
		}

		// Initialize the globals associated with the Virtual Origin
		coordinates = new float[RATES.get(RATE)][DEGREES] ;
		counter_origin = 0 ;
		counter_orient = 0 ;
		northAzimuth = 0 ;
		northInclination = 0;
	}
	
	/**
	 * Helper function to set the virtual origin for all coordinate systems
	 * @param newOrigin - a new virtual origin (x, y, z, inclination, azimuth values)
	 */
	private void setVirtualOrigins(float[] newOrigin) {
		rawCoordinates.setVirtualOrigin(newOrigin);
		absoluteCoordinates.setVirtualOrigin(newOrigin);
		sphericalCoordinates.setVirtualOrigin(newOrigin);
		lpfCoordinates.setVirtualOrigin(newOrigin);
		hpfCoordinates.setVirtualOrigin(newOrigin);
	}
	
	/**
	 * Helper function to collect the specified number of data entries for a
	 *  set of readings (from the accelerometer) and then averages them.
	 *  
	 * @param num - the number of data entries to collect for an average
	 * @return - the averaged virtual origin
	 */
	private float[] findVirtualOrigin() {
		
		DecimalFormat df = new DecimalFormat("####.##");
		float dataSets[][] = new float[RATES.get(RATE)][DEGREES];
		float average[] = new float[DEGREES+2]; 	// 2 being the addition
													// of inclination and azimuth
		
		// Keep track in the first for loop for how many entries there actually were
		int actualEntries ;
		
		// Collect the specified number of data sets
		for(actualEntries=0; actualEntries < RATES.get(RATE); actualEntries++) {
			// If the float fields were not filled, break out of the loop
			if(	(coordinates[actualEntries][0] == 0.0f) && 
				(coordinates[actualEntries][1] == 0.0f) && 
				(coordinates[actualEntries][2] == 0.0f)	) {
				actualEntries-- ;
				break ;
			} else {
				dataSets[actualEntries] = coordinates[actualEntries] ;
			}
		}
		
		// Add up all the x, y, and z values respectively
		for(int i=0; i < actualEntries; i++) {
			average[0] += dataSets[i][0] ;
			average[1] += dataSets[i][1] ;
			average[2] += dataSets[i][2] ;
		}
		
		// Divide the x, y, and z values by the actual number of entries
		average[0] = average[0] / actualEntries ;
		average[1] = average[1] / actualEntries ;
		average[2] = average[2] / actualEntries ;
		
		// Prevent writing to the Logger when there were no entries collected
		if (actualEntries == 0) {
			return average ;
		}
		
		// Log the number of values collected
		Log.i("M100Dev", "Entries collected during origin calibration: " 
				+ String.valueOf(actualEntries));
		
		// Set the new simulated North's inclination and azimuth
		average[3] = (northInclination/counter_orient);		
		average[4] = (northAzimuth/counter_orient) ;
		
		// Log the virtual origin
		Log.i("M100Dev", "New Virtual Origin: " 	
				+ 	String.valueOf(df.format(average[0])) + " "
				+	String.valueOf(df.format(average[1])) + " "
				+ 	String.valueOf(df.format(average[2])) + " "
				+ 	String.valueOf(df.format(average[3])) + " "
				+ 	String.valueOf(df.format(average[4])));
		
		// If the azimuth is zero, indicate that there could be an issue 
		//  with the magnetic sensor
		if (average[4] == 0) {
			Log.e("M100Dev", "WARNING: There could be an issue with the "
					+ "magnetometer. Recommended solution: Restart device.");
		}
		
		return average ;
	}

	/**
	 * Used to reset the virtual origin.  Collects accelerometer coordinates
	 *  as well as an inclination and azimuth.  Uses multi-threading to execute
	 *  while retaining proper functionality for onSensorChanged (prevents the
	 *  functionality that stores and calculates the virtual origin from 
	 *  possessing a lock and abstaining access to the onSensorChanged method).
	 *  
	 * @param view
	 */
	public void doResetOrigin(View view) {
		final Context original = this ;
		
		// Start a new thread to run in the background
		Thread collect = new Thread() {
			// Toast messages to inform of the background processes
			Toast start = Toast.makeText(original, "Calibrating virtual origin...Do not move device...", Toast.LENGTH_LONG);
			Toast finish = Toast.makeText(original, "Calibration of virtual origin complete.", Toast.LENGTH_SHORT);
			public void run() {
				start.show();
				doCollect = true ;
				try {
					// Wait a specified number of seconds to collect sensor data
					Thread.sleep(WAIT_TIME_ORIGIN); // 3500 is the same waiting period as Toast.Length_LONG
					// Set the virtual origin with the newly collected data
					setVirtualOrigins(findVirtualOrigin());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				doCollect = false ;
				// Reset the counter_origin and values for the next calibration
				coordinates = new float[RATES.get(RATE)][DEGREES] ;
				counter_origin = 0 ;
				counter_orient = 0 ;
				northAzimuth = 0 ;
				northInclination = 0 ;
				
				/*
				 *  Change the views by the original thread that created the view hierarchy
				 *  < http://stackoverflow.com/questions/5161951/android-only-the-original-
				 *   thread-that-created-a-view-hierarchy-can-touch-its-vi >
				 */
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// Enable the buttons relying on absolute coordinates
						Button spherical 	= (Button) findViewById(R.id.btn_spherical);
						Button lpf 			= (Button) findViewById(R.id.btn_lpf);
						Button hpf 			= (Button) findViewById(R.id.btn_hpf);
						if (!spherical.isEnabled()) {
							spherical.setEnabled(true);
							lpf.setEnabled(true);
							hpf.setEnabled(true);
						}
					}
				}) ;
				
				finish.show();
			}
			
		};
		collect.start();
		
		
	}
	
	/**
	 * Function to start the view to display Raw Coordinates
	 * @param view
	 */
	public void gotoRawCS(View view) {
		Intent sensorIntent = new Intent(this, SensorActivity.class);
		sensorIntent.putExtra("coordinate_system", rawCoordinates);
		
    	startActivity(sensorIntent);		
	}
	
	/**
	 * Function to start the view to display Absolute Coordinates
	 * @param view
	 */
	public void gotoAbsoluteCS(View view) {
		Intent sensorIntent = new Intent(this, SensorActivity.class);
		sensorIntent.putExtra("coordinate_system", absoluteCoordinates);
		
		startActivity(sensorIntent);	
	}
	
	/**
	 * Function to start the view to display Spherical Coordinates
	 * @param view
	 */
	public void gotoSphericalCS(View view) {
		Intent sensorIntent = new Intent(this, SensorActivity.class);
		sensorIntent.putExtra("coordinate_system", sphericalCoordinates);
		
    	startActivity(sensorIntent);	
	}
	
	/**
	 * Function to start the view to display Spherical (LPF) Coordinates
	 * @param view
	 */
	public void gotoLowPassFilterCS(View view) {
		Intent sensorIntent = new Intent(this, SensorActivity.class);
		sensorIntent.putExtra("coordinate_system", lpfCoordinates);
		
    	startActivity(sensorIntent);	
	}
	
	/**
	 * Function to start the view to display Spherical (HPF) Coordinates
	 * @param view
	 */
	public void gotoHighPassFilterCS(View view) {
		Intent sensorIntent = new Intent(this, SensorActivity.class);
		sensorIntent.putExtra("coordinate_system", hpfCoordinates);
		
    	startActivity(sensorIntent);	
	}
	
	/**
	 * Helper function to set the rate at which the sensors collect data
	 * @param rate - provided from SensorManager.SENSOR_DELAY_*
	 */
	private void setSensorRates(int rate) {
		mSensorManager.registerListener(this, sensorAccelerometer, rate);
		mSensorManager.registerListener(this, sensorMagnetic, rate);
		mSensorManager.registerListener(this, sensorGravity, rate);
	}

	/////////////////////////////////////////////////////////////
	// Below methods are for the interface SensorEventListener //
	/////////////////////////////////////////////////////////////
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int arg1) {
		Log.w("M100Dev","Unaccounted action with: "+ sensor.getName());
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (doCollect) {
			if ( (event.sensor.getType() == Sensor.TYPE_GRAVITY)){
				// Apply a low pass filter to the gravity vector values
				mGravity = lpfCoordinates.filter(event.values, mGravity) ;
			} else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				// Keep on an ongoing record of the last 10 values of the accelerometer
				coordinates[counter_origin % RATES.get(RATE)][0] = event.values[0] ;
				coordinates[counter_origin % RATES.get(RATE)][1] = event.values[1] ;
				coordinates[counter_origin % RATES.get(RATE)][2] = event.values[2] ;
				counter_origin++;

				// Keep track of if recording above/below the raw origin's inclination
				if (event.values[2] > 0) {
					sign = -1 ;
				} else {
					sign = 1;
				}

			} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				// Apply a low pass filter to the magnetic values
				mMagnetic = lpfCoordinates.filter(event.values, mMagnetic) ;
			}

			// If values have been found for both the gravity and magnetic sensor
			if ( (mGravity != null && mMagnetic != null) ) {
				// Keep track of how many times accessing this portion of code
				counter_orient++ ;
				
				// Get the rotation matrix for the azimuth (requires modification
				//  of the ordering of the values due to axes differences
				SensorManager.getRotationMatrix(mRotation, mInclination, 
						new float[]{mGravity[0], -mGravity[2], mGravity[1]}, 
						new float[]{mMagnetic[0], -mMagnetic[2], mMagnetic[1]});
				SensorManager.getOrientation(mRotation, mValues) ;
				// Add up all the azimuth values to average out later
				northAzimuth += (mValues[0] * RAD_TO_DEG) ;

				// Get the rotation matrix for the inclination
				SensorManager.getRotationMatrix(mRotation, mInclination, mGravity, mMagnetic);
				SensorManager.getOrientation(mRotation, mValues) ;
				// Add up all the inclination values to average out later
				northInclination += (float) (sign * ((mValues[1] * RAD_TO_DEG + 90 )));
			}
		}
	}
}
