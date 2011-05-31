package com.lg.accel;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ORIENTATION;
import static android.hardware.SensorManager.SENSOR_DELAY_UI;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AccelTest extends Activity implements SensorEventListener, OnClickListener
{
	private SensorManager director;
	private TextView display;
	private Button calibrationButton;
	private float x1 = 0, y1 = 0, z1 = 0, x2 = 0, y2 = 0, z2 = 0, xFinal, yFinal, zFinal;
	private float dx, dy, dz;
	String s = "X: 0\nY: 0\nZ: 0\n\nX: 0\nY: 0\nZ: 0";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        display = (TextView) findViewById(R.id.display);
        display.setText(s);
        calibrationButton = (Button) findViewById(R.id.calibration_button);
        calibrationButton.setOnClickListener(this);
    }

	protected void onPause()
	{
		super.onPause();
		director.unregisterListener(this, director.getDefaultSensor(TYPE_ACCELEROMETER));
		director.unregisterListener(this, director.getDefaultSensor(TYPE_ORIENTATION));
		director = null;
	}
	
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
		synchronized(this)
		{
			if(event.sensor.getType() == TYPE_ORIENTATION)
			{
				x2 = event.values[0];
				y2 = event.values[1];
				z2 = event.values[2];
				xFinal = x2 - dx;
				yFinal = y2 - dy;
				zFinal = z2 - dz;
				s = "X: " + x1 + "\nY: " + y1 + "\nZ: " + z1 + "\n\nX: " + xFinal + "\nY: " + yFinal + "\nZ: " + zFinal;
				
				display.setText(s);
			}
			if(event.sensor.getType() == TYPE_ACCELEROMETER)
			{
				x1 = event.values[0];
				y1 = event.values[1];
				z1 = event.values[2];
				s = "X: " + x1 + "\nY: " + y1 + "\nZ: " + z1 + "\n\nX: " + xFinal + "\nY: " + yFinal + "\nZ: " + zFinal;
					
				display.setText(s);
			}
		}
	}
	
	public void onClick(View v)
	{
		if(v == calibrationButton)
		{
			dx = x2;
			dy = y2;
			dz = z2;
		}
	}
}