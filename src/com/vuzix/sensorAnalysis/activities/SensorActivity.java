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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
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
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vuzix.sensorAnalysis.R;
import com.vuzix.sensorAnalysis.functions.AbsoluteCS;
import com.vuzix.sensorAnalysis.functions.CoordinateSystem;
import com.vuzix.sensorAnalysis.functions.HighPassFilterCS;
import com.vuzix.sensorAnalysis.functions.LowPassFilterCS;
import com.vuzix.sensorAnalysis.functions.RawCS;
import com.vuzix.sensorAnalysis.functions.SphericalCS;

/**
 * The main functionality of the application is contained in SensorActivity
 * 
 * Defines several views based on the com.vuzix.sensorAnalysis.functions.
 * Dynamically loads each view and executes unique code for each coordinate system.
 * 
 * The main functionality is to display X (or Magnitude), Y (or Inclination), and 
 *  Z (or Azimuth) values.  Provides additional functionality to display CPU usage
 *  of the app and the ability to modify the sensor's rate of collection.
 * 
 * @author Connor Hack <connor_hack@vuzix.com>
 *
 */
public class SensorActivity extends Activity implements SensorEventListener {

	// Global variables declared here
	private static String PACKAGE_NAME ;
	private static Activity ORIGINAL_ACTIVITY ;
	
	private SensorManager mSensorManager;
	private Sensor sensorAccelerometer, sensorGravity, 
					sensorMagnetic, sensorAcceleration ;
	private CoordinateSystem coordinateSystem ;
	private static int RATE = MainActivity.DELAY_FASTEST ;
	
	// Globals for the CPU thread
	private Thread threadCPU ;
	private boolean cpuRunning, paused ;
	
	// Globals for onSensorChanged
	private float mGravity[] = new float[3] ;
	private float mMagnetic[] = new float[3] ;
	private float mInclination[] = new float[9] ;
	private float mRotation[] = new float[9] ;
	private float values[] = new float[3] ;
	
	/////////////////////////////////////////////////////
	// Below methods are for the extension of Activity //
	/////////////////////////////////////////////////////
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensors);
		
		// Enable the home as up from this activity
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		initGlobals();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//TODO Keep if needed/wanted in the future
		/*// Identify if the android device is charging/plugged in
		Intent intent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        // If the device is plugged in, increase the rate at which the sensors collect data
		if (plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB) {
			RATE = SensorManager.SENSOR_DELAY_FASTEST;
			Log.i("M100Dev", "Device charging; changing sensor delay to fastest speed.");
		}*/
		
		setSensorRates(RATE);

		// Indicate the CPU usage thread is no longer paused
		paused = false ;
		
		// Interrupt the waiting thread to indicate it can resume execution
		if (threadCPU.getState() == Thread.State.TIMED_WAITING) {
			threadCPU.interrupt();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// Unregister the sensors and pause the CPU usage thread
		mSensorManager.unregisterListener(this);
		paused = true ;
	}
	
	@Override
	protected void onDestroy() {
		super.onStop();
		
		// Terminate the thread processing the CPU usage; unpause the thread
		//  to allow it to finish.
		paused = false ;
		cpuRunning = false ;
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
	    
	    // Set the next left focus to be the home (back) button 
	    spinner.setNextFocusLeftId(android.R.id.home);
	    
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
                		RATE = MainActivity.DELAY_NORMAL ;
                		toast.setText("Sensor rate set to Slowest Rate.");
                		
                	} else if(item.toString().equals(mContext.getResources().getString(R.string.rate_ui))) {
                		// Set the sensor rate to the UI mode
                		RATE = MainActivity.DELAY_UI ;
                		toast.setText("Sensor rate set to Slow Rate.");
                		
                	} else if(item.toString().equals(mContext.getResources().getString(R.string.rate_game))) {
                		// Set the sensor rate to the Game mode
                		RATE = MainActivity.DELAY_GAME ;
                		toast.setText("Sensor rate set to Normal Rate.");
                		
                	} else if(item.toString().equals(mContext.getResources().getString(R.string.rate_fastest))) {
                		// Set the sensor rate to the Fastest mode
                		RATE = MainActivity.DELAY_FASTEST ;
                		toast.setText("Sensor rate set to Fast Rate.");
                	}
                	
                	// Reset the sensors that are dependent on the sensor rate
                	setSensorRates(RATE);
                	toast.show();
                }
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
	    
		// Set the initial selection at the middle value
		spinner.setSelection((int) (Math.floor( (MainActivity.RATES.size()-1)/2) ));
		
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// If the home button was pressed, finish this activity
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	////////////////////////////////////////////////
	// Below are methods specific to MainActivity //
	////////////////////////////////////////////////
	
	/**
	 * Helper function to organize code
	 * 
	 * Used to initialize global variables used throughout this module
	 */
	private void initGlobals() {
		
		// Store the package name and context for the CPU thread
		PACKAGE_NAME = getApplicationContext().getPackageName();
		ORIGINAL_ACTIVITY = this ;
		
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		
		// Attempt to instantiate the sensors
		sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		sensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		
		// Identify if there does not exist an accelerometer on the device
		if (sensorAccelerometer == null)
			Log.w("M100Dev", "Device does not possess an accelerometer.");
		if (sensorGravity == null)
			Log.w("M100Dev", "Device does not possess a gravity sensor.");
		if (sensorMagnetic == null)
			Log.w("M100Dev", "Device does not possess a magnetic field sensor.");
		if (sensorAcceleration == null)
			Log.w("M100Dev", "Device does not possess a magnetic field sensor.");
		
		// From the intent, retrieve the parcelable coordinate system extra
		Bundle data = getIntent().getExtras();
		coordinateSystem = data.getParcelable("coordinate_system");
		
		identifyCSType() ;
		
		cpuRunning = true ;

		// Create the thread that will find the CPU usage on the device
		threadCPU = new Thread() {
			public void run() {

				// Keep looping while the activity has not been stopped
				while (cpuRunning) {
					if (paused) {
						// If the activity is paused
						try {
							// Tell the thread to sleep for 30 seconds and 
							//  if it is still running in that time, finish
							//  the activity.
							Thread.sleep(30000);
							ORIGINAL_ACTIVITY.finish();
						} catch (InterruptedException ie) {
							Log.w("M100_Exceptions", "Caught a thrown interruption: " + ie.toString());
						}
					} else {
						
						try {
							// Run from the adb shell a command to grab the top
							//  ten processes taking up CPU
							Process p = Runtime.getRuntime().exec("top -n 1 -m 10");

							// Wait for the thread to finish executing
							p.waitFor();

							// Initialize the input stream and reader to parse
							//  the message from executing <p> command
							BufferedInputStream buffer = new BufferedInputStream( p.getInputStream() );
							BufferedReader commandOutput= new BufferedReader( new InputStreamReader( buffer ) );
							
							// Attempt to parse each string produced from the command
							parseCPU(commandOutput);
							commandOutput.close(); 

						} catch (InterruptedException ie) {
							ie.printStackTrace();
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}
				}
			}
		};
		threadCPU.start();
	}
	
	private void parseCPU(BufferedReader commandOutput) throws IOException {
		
		String line = null ;
		
		// Read each line from the reader that is used to parse commands
		while ( ( line = commandOutput.readLine() ) != null ) {
			// Split the string by spaces
			final String[] result = line.split(" ");
			
			// If the last string from the split is the same as the package name
			if (result[result.length-1].equalsIgnoreCase(PACKAGE_NAME)) {
				// Run a thread on the UI thread to update views
				runOnUiThread(new Runnable() {
				     @Override
				     public void run() {
				    	 
				    	 int i ;
				    	 // Establish the position in the string array where
				    	 //  our CPU usage percentage is
				    	 for(i=0; i<result.length; i++) {
				    		 if ( (result[i].length() != 0) && 
				    				 (result[i].charAt(result[i].length()-1) == '%') ) {
				    			 break ;
				    		 }
				    	 }
				    	 
				    	 // Change the CPU value TextView to the digits
				    	 //  from the parsed string (excludes the % sign)
				    	 if (i != result.length) {
				    		 ((TextView) findViewById(R.id.tv_cpu_value))
					    	 	.setText(result[i].subSequence(0, result[i].length()-1));
				    	 } else {
				    		 // If the process is not in the top ten processes,
				    		 //  set the value to zero
				    		 ((TextView) findViewById(R.id.tv_cpu_value))
					    	 	.setText(0);
				    	 }
				    	 
				    }
				});
				// Discontinue parsing
				break ;
			}
		}
	}
	
	/**
	 * Helper function used to identify the type of coordinate system currently in use
	 */
	private void identifyCSType() {
		
		String types[] = {"RawCS", "AbsoluteCS", "SphericalCS", "LowPassFilterCS", "HighPassFilterCS"};
		
		// Confirm the coordinate system has been instantiated
		if(coordinateSystem == null) {
			Log.e("M100Dev", "No coordinate system was passed in the intent.");
			return ;
		}
		
		// Compare the strings to those that represent the types of CS classes.
		if(coordinateSystem.toString().equals(types[0])) {
			coordinateSystem = (RawCS) coordinateSystem ;
			this.setTitle(R.string.activity_raw);
		} else if(coordinateSystem.toString().equals(types[1])) {
			coordinateSystem = (AbsoluteCS) coordinateSystem ;
			this.setTitle(R.string.activity_absolute);
		} else if(coordinateSystem.toString().equals(types[2])) {
			coordinateSystem = (SphericalCS) coordinateSystem ;
			this.setTitle(R.string.activity_spherical);
		} else if(coordinateSystem.toString().equals(types[3])) {
			coordinateSystem = (LowPassFilterCS) coordinateSystem ;
			this.setTitle(R.string.activity_lpf);
		} else if(coordinateSystem.toString().equals(types[4])) {
			coordinateSystem = (HighPassFilterCS) coordinateSystem ;
			this.setTitle(R.string.activity_hpf);
		}
		
		updateUI(coordinateSystem, 0, 0, 0);
	}
	
	/**
	 * Helper function to set the rate at which the sensors collect data
	 * @param rate - provided from SensorManager.SENSOR_DELAY_*
	 */
	private void setSensorRates(int rate) {
		mSensorManager.registerListener(this, sensorAccelerometer, rate);
		mSensorManager.registerListener(this, sensorGravity, rate);
		mSensorManager.registerListener(this, sensorMagnetic, rate);
		mSensorManager.registerListener(this, sensorAcceleration, rate);
	}
	
	/**
	 * Massive helper function to set the user interface
	 * 
	 * Sets each text for each TextView that depends on a Coordinate System
	 * Also modifies the margins of the View if the CS is an instance of SphericalCS
	 * 
	 * @param cs - A coordinate system
	 * @param x - The x value to set in a TextView
	 * @param y - The y value to set in a TextView
	 * @param z - The z value to set in a TextView
	 */
	private void updateUI(CoordinateSystem cs, float x, float y, float z) {
		
		// Set the format for printing out values
		DecimalFormat df = new DecimalFormat("####.##");
		
		// Create TextViews for each dynamic TextView
		TextView 	acc_x, acc_x_val, acc_x_unit,
					acc_y, acc_y_val, acc_y_unit,
					acc_z, acc_z_val, acc_z_unit ;
		
		// Find each view by their specified ID
		acc_x 		=	((TextView) findViewById(R.id.tv_acceleration_x)) ;
		acc_y 		= 	((TextView) findViewById(R.id.tv_acceleration_y)) ;
		acc_z 		= 	((TextView) findViewById(R.id.tv_acceleration_z)) ;
		acc_x_val 	=	((TextView) findViewById(R.id.tv_acceleration_x_value)) ;
		acc_y_val 	= 	((TextView) findViewById(R.id.tv_acceleration_y_value)) ;
		acc_z_val 	= 	((TextView) findViewById(R.id.tv_acceleration_z_value)) ;
		acc_x_unit 	=	((TextView) findViewById(R.id.tv_acceleration_x_units)) ;
		acc_y_unit 	= 	((TextView) findViewById(R.id.tv_acceleration_y_units)) ;
		acc_z_unit 	=  	((TextView) findViewById(R.id.tv_acceleration_z_units)) ;
		
		// Set the description text for each TextView
		acc_x.setText(cs.getDesc("x"));
		acc_y.setText(cs.getDesc("y"));
		acc_z.setText(cs.getDesc("z"));

		// Set the value for each TextView
		acc_x_val.setText(df.format(x));
		acc_y_val.setText(df.format(y));
		acc_z_val.setText(df.format(z));

		// Set the units for each TextView
		acc_x_unit.setText(cs.getAccUnits("x"));
		acc_y_unit.setText(cs.getAccUnits("y"));
		acc_z_unit.setText(cs.getAccUnits("z"));
		
		// If the coordinate system is an instance of SphericalCS, modify the margins
		// We want this section to only be called once, so within identifyCSTYpe we
		//  call this method with x/y/z all zero.  Since the probability of X, Y, and
		//  Z all being zero is incredibly low, this is a relatively safe procedure.
		if ( (cs instanceof SphericalCS) && ( (x == 0) && (y == 0) && (z == 0) )){
			
			// Find each LayoutParameter for each related TextView
			LayoutParams acc_x_rlp 		= (LayoutParams) acc_x_val.getLayoutParams() ;
			LayoutParams acc_y_rlp 		= (LayoutParams) acc_y_val.getLayoutParams() ;
			LayoutParams acc_z_rlp 		= (LayoutParams) acc_z_val.getLayoutParams() ;
			LayoutParams acc_x_unit_rlp = (LayoutParams) acc_x_unit.getLayoutParams() ;
			LayoutParams acc_y_unit_rlp = (LayoutParams) acc_y_unit.getLayoutParams() ;
			LayoutParams acc_z_unit_rlp = (LayoutParams) acc_z_unit.getLayoutParams() ;
			
			// setMargins(left, top, right, bottom)
			// Set the margins for each TextView
			acc_x_rlp.setMargins(15, 0, 0, 0);
			acc_y_rlp.setMargins(15, 0, 0, 0);
			acc_z_rlp.setMargins(33, 0, 0, 0);
			acc_x_unit_rlp.setMargins(210, 0, 0, 0);
			acc_y_unit_rlp.setMargins(210, 0, 0, 0);
			acc_z_unit_rlp.setMargins(210, 0, 0, 0);
			
			// Apply each new LayoutParam
			acc_x_val.setLayoutParams(acc_x_rlp);
			acc_y_val.setLayoutParams(acc_y_rlp);
			acc_z_val.setLayoutParams(acc_z_rlp);
			acc_x_unit.setLayoutParams(acc_x_unit_rlp);
			acc_y_unit.setLayoutParams(acc_y_unit_rlp);
			acc_z_unit.setLayoutParams(acc_z_unit_rlp);
		}
	}

	/////////////////////////////////////////////////////////////
	// Below methods are for the interface SensorEventListener //
	/////////////////////////////////////////////////////////////
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Report any changes to sensors' accuracy
		Log.w("M100Dev","ACCURACY CHANGED: " + sensor.getName() + ": " + String.valueOf(accuracy));
	}
	
	// Override the method to capture the sensors' changes
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		// if the CS is of type Spherical, find the orientation
		if (coordinateSystem instanceof SphericalCS) {
			if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
				// If the CS is of a type to apply a HPF or a LPF, filter the 
				//  found values by the appropriate filter
				if (coordinateSystem instanceof LowPassFilterCS) {
					mGravity = ((LowPassFilterCS) coordinateSystem).filter(event.values, mGravity);
				} else if (coordinateSystem instanceof HighPassFilterCS) {
					mGravity = ((HighPassFilterCS) coordinateSystem).filter(event.values, mGravity);
				} else {
					// Else just store the values
					mGravity = event.values ;
				}
			} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				// If the CS is of a type to apply a HPF or a LPF, filter the 
				//  found values by the appropriate filter
				if (coordinateSystem instanceof LowPassFilterCS) {
					mMagnetic = ((LowPassFilterCS) coordinateSystem).filter(event.values, mMagnetic);
				} else if (coordinateSystem instanceof HighPassFilterCS) {
					mMagnetic = ((HighPassFilterCS) coordinateSystem).filter(event.values, mMagnetic);
				} else {
					// Else just store the values
					mMagnetic = event.values ;
				}
			} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				// Store the values within the CS to calculate the magnitude component
				((SphericalCS) coordinateSystem).setValueX(event.values);
			}
			
			// If values have been found for both the gravity and magnetic sensor
			if (mGravity != null && mMagnetic != null) {
				
				// Get the rotation matrix for the azimuth (requires modification
				//  of the ordering of the values due to axes differences)
				SensorManager.getRotationMatrix(mRotation, mInclination, 
						new float[]{mGravity[0], -mGravity[2], mGravity[1]}, 
						new float[]{mMagnetic[0], -mMagnetic[2], mMagnetic[1]});
				SensorManager.getOrientation(mRotation, values);
				// Store the value found by the two methods above as the azimuth
				((SphericalCS) coordinateSystem).setValueZ(values[0]);
				
				// Get the rotation matrix for the inclination
				SensorManager.getRotationMatrix(mRotation, mInclination, mGravity, mMagnetic) ;
				SensorManager.getOrientation(mRotation, values) ;
				// Store the value found by the two methods above as the inclination
				((SphericalCS) coordinateSystem).setValueY((float) (((values[1]*180) / Math.PI) + 90) );
			}
		}
		
		// Filter out all other sensors besides the accelerometer and linear acc.
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			if (! (coordinateSystem instanceof AbsoluteCS) ){
				// Give accelerometer values to all instances not of AbsoluteCS
				coordinateSystem.setValues(event.values);
			}
		} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			if (coordinateSystem instanceof AbsoluteCS) {
				// Give linear acceleration values to the AbsoluteCS
				coordinateSystem.setValues(event.values);
			}
		} else {
			// Don't allow other sensors to update the UI
			return ;
		}
		
		// Pass in parameters to modify the UI
		updateUI(coordinateSystem, coordinateSystem.getValueX() , 
								coordinateSystem.getValueY() ,
								coordinateSystem.getValueZ() );
		
	}
}
