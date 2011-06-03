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
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;

/** Implements SensorEventListener for the accelerometer/orientation values
* Implements OnGestureListener for the scrolling (touch-screen) values */
public class AccelTest extends Activity implements SensorEventListener, OnGestureListener
{
	private static final String TAG = "MyActivity"; //For debugging purposes
	private SensorManager director;
	private TextView display1, display2;
	private GestureDetector detector;
	private float x1 = 0, y1 = 0, z1 = 0, x2 = 0, y2 = 0, z2 = 0, xFinal, yFinal, zFinal;
	private float dx = 0, dy = 0, dz = 0;
	private String s1 = "X: 0\nY: 0\nZ: 0\n\nX: 0\nY: 0\nZ: 0";
	private String s2 = "\nScroll info:\nX: 0\nY: 0";
	protected static final int SUB_ACTIVITY_REQUEST_CODE = 100;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        display1 = (TextView) findViewById(R.id.display1);
        display2 = (TextView) findViewById(R.id.display2);
        display1.setText(s1);
        display2.setText(s2);
        detector = new GestureDetector(this, this); //Initializes the GestureDetector (for touch-screen input)
        detector.setIsLongpressEnabled(false);
    }

    /** Called when the application is paused (essentially any time that the user navigates away from the main activity) */
	protected void onPause()
	{
		super.onPause();
		
		//Called so that the sensors do not drain the battery when not in use
		director.unregisterListener(this, director.getDefaultSensor(TYPE_ACCELEROMETER));
		director.unregisterListener(this, director.getDefaultSensor(TYPE_ORIENTATION));
		director = null;
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
	
	/** Creates the menu */
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	/** Determines what to do when the user chooses a menu option */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()){
		case R.id.calibrate:
		{
			Intent i = new Intent(AccelTest.this, Calibrate.class);
			startActivityForResult(i, SUB_ACTIVITY_REQUEST_CODE);
		}
			return true;
		case R.id.help:
		{
			Intent i = new Intent(AccelTest.this, Help.class);
			startActivity(i);
		}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/** Determines what to do with the sub-activity's results */
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == SUB_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK)
		{
			Bundle b = data.getExtras();
			dx = b.getFloat("0");
			dy = b.getFloat("1");
			dz = b.getFloat("2");
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		//As of now this method really doesn't need to do anything
	}
	
	/** Called whenever any values from the sensors change */
	public void onSensorChanged(SensorEvent event)
	{
		synchronized(this) //I still don't know for sure if this line is necessary. If anyone reading my code knows, please inform me.
		{
			if(event.sensor.getType() == TYPE_ORIENTATION)
			{
				//The raw, uncalibrated values
				x2 = event.values[0];
				y2 = event.values[1];
				z2 = event.values[2];
				
				//The calibrated values which have been adjusted by the appropriate offset
				xFinal = x2 - dx;
				yFinal = y2 - dy;
				zFinal = z2 - dz;
				s1 = "X: " + x1 + "\nY: " + y1 + "\nZ: " + z1 + "\n\nX: " + xFinal + "\nY: " + yFinal + "\nZ: " + zFinal;;
				
				display1.setText(s1); //Updates the display
			}
			if(event.sensor.getType() == TYPE_ACCELEROMETER)
			{
				x1 = event.values[0];
				y1 = event.values[1];
				z1 = event.values[2];
				s1 = "X: " + x1 + "\nY: " + y1 + "\nZ: " + z1 + "\n\nX: " + xFinal + "\nY: " + yFinal + "\nZ: " + zFinal;
					
				display1.setText(s1); //Updates the display
			}
		}
	}
	
	//a method of the View class which needs to be implemented for touch control to work
	public boolean onTouchEvent(MotionEvent me)
	{ 
		//Passes knowledge of the MotionEvent to the detector, which in turn allows the other OnGestureListener
		//    methods to be called
		return detector.onTouchEvent(me); 
	}
	
	public boolean onDown(MotionEvent e)
	{
		return true;
	}
	
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		return true;
	}
	
	public void onLongPress(MotionEvent e){}
	
	public void onShowPress(MotionEvent e){}
	
	public boolean onSingleTapUp(MotionEvent e)
	{
		return true;
	}
	
	/** Called when a scroll motion is made on the touch-screen */
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
	{
		String s2 = "\nScroll info:\nX: " + distanceX + "\nY: " + distanceY;
		display2.setText(s2);
		
		return true;
	}
}