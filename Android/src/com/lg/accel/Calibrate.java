/** Reese Butler
 *  6/2/2011
 */

package com.lg.accel;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ORIENTATION;
import static android.hardware.SensorManager.SENSOR_DELAY_UI;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Calibrate extends Activity implements SensorEventListener, OnClickListener
{
	private SensorManager director;
	private Button calibrationButton;
	private TextView calScreen;
	private float dx, dy, dz;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calibrate);
		calScreen = (TextView) findViewById(R.id.calibrate);
		calibrationButton = (Button) findViewById(R.id.calibration_button);
		calScreen.setText("Place the phone in the desired neutral orientation (i.e. on a flat table) and press \"Calibrate\".\n");
		calibrationButton.setOnClickListener(this);
	}
	
	/** Called when the application is paused (essentially any time that the user navigates away from the main activity) */
	protected void onPause()
	{
		super.onPause();
		
		//Called so that the sensors do not drain the battery when not in use
		director.unregisterListener(this, director.getDefaultSensor(TYPE_ACCELEROMETER));
		director.unregisterListener(this, director.getDefaultSensor(TYPE_ORIENTATION));
		director = null;
		setResult(RESULT_CANCELED);
		finish();
	}
	
	/** Called after the application is started or resumed */
	protected void onResume()
	{
		super.onResume();
		director = (SensorManager) getSystemService(SENSOR_SERVICE);
		boolean accelExists = director.registerListener(this, director.getDefaultSensor(TYPE_ACCELEROMETER), SENSOR_DELAY_UI);
		boolean orientExists = director.registerListener(this, director.getDefaultSensor(TYPE_ORIENTATION), SENSOR_DELAY_UI);
		
		if(!accelExists)
		{
			director.unregisterListener(this, director.getDefaultSensor(TYPE_ACCELEROMETER));
		}
		
		if(!orientExists)
		{
			director.unregisterListener(this, director.getDefaultSensor(TYPE_ORIENTATION));
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		//As of now this method really doesn't need to do anything
	}
	
	public void onSensorChanged(SensorEvent event)
	{
		if(event.sensor.getType() == TYPE_ORIENTATION)
		{
			dx = event.values[0];
			dy = event.values[1];
			dz = event.values[2];
		}
	}
	
	/** Called when any section of the Activity's display is clicked */
	public void onClick(View v)
	{
		if(v == calibrationButton) //If the button was pressed, return the calibration values
		{
			Intent data = new Intent();
			Bundle b = new Bundle();
			b.putFloat("0", dx);
			b.putFloat("1", dy);
			b.putFloat("2", dz);
			data.putExtras(b);
			setResult(RESULT_OK, data);
			finish();
		}
	}
}