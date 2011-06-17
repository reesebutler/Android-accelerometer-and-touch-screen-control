/** Reese Butler
 *  6/13/2011
 */

package com.lg.accel;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ORIENTATION;
import static android.hardware.SensorManager.SENSOR_DELAY_UI;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

/** Implements SensorEventListener for the accelerometer/orientation values
* Implements OnGestureListener for the scrolling (touch-screen) values */
public class AccelTest extends Activity implements SensorEventListener, OnGestureListener, OnClickListener, OnTouchListener
{
	private static final String TAG = "MyActivity"; //For debugging purposes
	private SensorManager director;
	private GestureDetector detector;
	
	@SuppressWarnings("unused")
	private float x1 = 0, y1 = 0, z1 = 0, x2 = 0, y2 = 0, z2 = 0, xFinal, yFinal, zFinal, distanceX, distanceY, panZ = 0, roll = 0;
	private float dx = 0, dy = 0, dz = 0;
	protected static final int SUB_ACTIVITY_REQUEST_CODE = 100;
	private static String IP = "192.168.1.100", port = "4444", dataString = "";
	private PrintWriter outToServer;
	private Socket clientSocket;
	private WifiManager wifi;
	private FileInputStream in = null;
	private InputStreamReader inReader = null;
	char[] inputBuffer = new char[255];
	private boolean connected = false, frozen = false, shouldBeConnected = false;
	private Button freezeButton, calibrateButton;
	private ImageButton cwiseButton, ccwiseButton, upButton, downButton;
	private ImageView connectivity_icon;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        freezeButton = (Button) findViewById(R.id.freeze);
        calibrateButton = (Button) findViewById(R.id.calibrate);
        cwiseButton = (ImageButton) findViewById(R.id.clockwise);
        ccwiseButton = (ImageButton) findViewById(R.id.counterclockwise);
        upButton = (ImageButton) findViewById(R.id.upz);
        downButton = (ImageButton) findViewById(R.id.downz);
        
        freezeButton.setOnClickListener(this);
        calibrateButton.setOnClickListener(this);
        cwiseButton.setOnTouchListener(this);
        ccwiseButton.setOnTouchListener(this);
        upButton.setOnTouchListener(this);
        downButton.setOnTouchListener(this);
        
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
		wifi = (WifiManager) getSystemService(WIFI_SERVICE);
		boolean accelExists = director.registerListener(this, director.getDefaultSensor(TYPE_ACCELEROMETER), SENSOR_DELAY_UI);
		boolean orientExists = director.registerListener(this, director.getDefaultSensor(TYPE_ORIENTATION), SENSOR_DELAY_UI);
		
		if(!connected)
		{
			this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		
		if(!accelExists)
		{
			director.unregisterListener(this, director.getDefaultSensor(TYPE_ACCELEROMETER));
		}
		
		if(!orientExists)
		{
			director.unregisterListener(this, director.getDefaultSensor(TYPE_ORIENTATION));
		}
		
		//Attempts to retrieve any previously stored IP address and port
		try{
			in = openFileInput("settings.dat");
			inReader = new InputStreamReader(in);
			inReader.read(inputBuffer);
			dataString = new String(inputBuffer);
			dataString = dataString.trim();
			IP = dataString.substring(0, dataString.indexOf(","));
			port = dataString.substring(dataString.indexOf(",") + 1);
		} catch (Exception e) {
			e.printStackTrace();
			Intent i = new Intent(AccelTest.this, Configure.class);
			startActivityForResult(i, SUB_ACTIVITY_REQUEST_CODE);
		} finally {
			try {
				if(inReader != null && in != null)
				{
					inReader.close();
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//Attempts to connect if the user pressed "connect" in the configure screen
		if(shouldBeConnected && !connected)
		{
			if(wifi.isWifiEnabled())
			{
		        try {
		        	clientSocket = new Socket(IP, Integer.parseInt(port));
		        	outToServer = new PrintWriter(clientSocket.getOutputStream(), true);
		        	connected = true;
		        	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		        	Toast.makeText(this, "Connected successfully", Toast.LENGTH_SHORT).show();
		        } catch (Exception e){ 
		        	Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_LONG).show();
		        	connected = false;
		        }
			}
			else
			{
				connected = false;
				Toast.makeText(this, "Please enable Wi-Fi to connect", Toast.LENGTH_LONG).show();
			}
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
		case R.id.help:
		{
			Intent i = new Intent(AccelTest.this, Help.class);
			startActivity(i);
		}
			return true;
		case R.id.configure:
		{
			Intent i = new Intent(AccelTest.this, Configure.class);
			startActivityForResult(i, SUB_ACTIVITY_REQUEST_CODE);
		}
			return true;
		case R.id.connect: //Initializes network communication 
		{
			shouldBeConnected = false;
			
			if(connected)
			{
				outToServer.close();
				
				try {
					clientSocket.close();
					Toast.makeText(this, "Successfully disconnected", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				connected = false;
			}
		}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/** Determines what to do with the sub-activity's (Calibrate) results */
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == SUB_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK)
		{
			shouldBeConnected = true;
		}
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		//This method really doesn't need to do anything
	}
	
	/** Called whenever any values from the sensors change */
	public void onSensorChanged(SensorEvent event)
	{
		synchronized(this) //I still don't know for sure if this line is completely necessary. If anyone reading my code knows, please inform me.
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
				
				//Sends the output to the server
				if(connected && !frozen)
					outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ); 
			}
			if(event.sensor.getType() == TYPE_ACCELEROMETER)
			{
				x1 = event.values[0];
				y1 = event.values[1];
				z1 = event.values[2];
				
				//Sends the output to the server
				if(connected && !frozen)
					outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ); 
			}
		}
	}
	
	//a method of the View class which needs to be implemented for touch control to work
	public boolean onTouchEvent(MotionEvent me)
	{
		Log.d(TAG, "This is in the onTouchEvent");
		//Resets scroll values to zero when the user stops touching the screen
		if(me.getAction() == MotionEvent.ACTION_UP)
			distanceX = 0; distanceY = 0;
			
		//Passes knowledge of the MotionEvent to the detector, which in turn allows the other OnGestureListener
		//    methods to be called
		return detector.onTouchEvent(me); 
	}
	
	/** Implemented to allow buttons to be pressed without a complete "onClick" call */
	public boolean onTouch(View v, MotionEvent me)
	{
		if(me.getAction() == MotionEvent.ACTION_UP)
		{
			panZ = 0;
			roll = 0;
		}
		
		if(v == cwiseButton && me.getAction() == MotionEvent.ACTION_DOWN)
			roll ++;
		else if(v == ccwiseButton  && me.getAction() == MotionEvent.ACTION_DOWN)
			roll --;
		else if(v == upButton && me.getAction() == MotionEvent.ACTION_DOWN)
			panZ ++;
		else if(v == downButton && me.getAction() == MotionEvent.ACTION_DOWN)
			panZ --;
		
		if(v == cwiseButton || v == ccwiseButton || v == upButton || v == downButton && me.getAction() == MotionEvent.ACTION_DOWN)
		{
			//Sends the output to the server
			if(connected && !frozen)
				outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ); 
		}
		
		return true;
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
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float newX, float newY)
	{
		distanceX = newX;
		distanceY = newY;
		
		//Sends the output to the server
		if(connected && !frozen)
			outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ); 
		
		return true;
	}
	
	/** Called when an area of the screen is clicked (i.e. a button) */
	public void onClick(View v)
	{
		if(v == freezeButton)
		{
			if(frozen)
				frozen = false;
			else
				frozen = true;
		}
		
		if(v == calibrateButton)
		{
			dx = x2;
			dy = y2;
			dz = z2;
		}
	}
}