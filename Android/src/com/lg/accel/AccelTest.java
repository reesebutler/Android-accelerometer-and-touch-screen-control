/** Reese Butler
 *  6/28/2011
 */

package com.lg.accel;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ORIENTATION;
import static android.hardware.SensorManager.*;

import java.io.BufferedReader;
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
public class AccelTest extends Activity implements SensorEventListener, OnClickListener, OnTouchListener
{
	private SensorManager director;
	//private GestureDetector detector;
	
	@SuppressWarnings("unused")
	private float x1 = 0, y1 = 0, z1 = 0, x2 = 0, y2 = 0, z2 = 0, xFinal, yFinal, zFinal, distanceX, distanceY, panZ = 0, roll = 0;
	private float dx = 0, dy = 0, dz = 0;
	private static final int SUB_ACTIVITY_REQUEST_CODE = 100;
	private static String IP = "192.168.1.100", port = "4444", dataString = "";
	private PrintWriter outToServer;
	private BufferedReader inFromServer;
	private Socket clientSocket;
	private WifiManager wifi;
	private FileInputStream in = null;
	private InputStreamReader inReader = null;
	char[] inputBuffer = new char[255];
	private boolean connected = false, frozen = false, shouldBeConnected = false, firstTimeFrozen = false;
	private Button freezeButton, calibrateButton;
	private ImageButton upButton, downButton;
	private ImageView connectivity_icon;
	private int panSens = 49, pitchSens = 49, rollSens = 49, zoomSpeed = 49, orientDisable = 0, invertX = 0, invertY = 0, invertPitch = 0, invertRoll = 0;
	
	//For touch control
	private long previousTime = 0, currentTime, diffTime;
	private float previousX = 0, previousY = 0, currentX, currentY, diffX, diffY;
	private boolean scrolling = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        connectivity_icon = (ImageView) findViewById(R.id.connectivity_icon);
        connectivity_icon.setAdjustViewBounds(true);
        connectivity_icon.setMaxHeight(45);
        connectivity_icon.setMaxWidth(45);
        connectivity_icon.setImageResource(R.drawable.red_icon);
        
        freezeButton = (Button) findViewById(R.id.freeze);
        calibrateButton = (Button) findViewById(R.id.calibrate);
        upButton = (ImageButton) findViewById(R.id.upz);
        downButton = (ImageButton) findViewById(R.id.downz);
        
        freezeButton.setOnClickListener(this);
        calibrateButton.setOnClickListener(this);
        upButton.setOnTouchListener(this);
        downButton.setOnTouchListener(this);
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
		boolean orientExists = director.registerListener(this, director.getDefaultSensor(TYPE_ORIENTATION), SENSOR_DELAY_GAME);
		
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
			in = openFileInput("connection.dat");
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
		
		//Attempts to retrieve any other previously stored settings
		try{
			in = openFileInput("settings.dat");
			inReader = new InputStreamReader(in);
			inReader.read(inputBuffer);
			dataString = new String(inputBuffer);
			dataString = dataString.trim();
			
			//Parses values for the settings
			panSens = Integer.parseInt(dataString.substring(0, 2));
			pitchSens = Integer.parseInt(dataString.substring(2, 4));
			rollSens = Integer.parseInt(dataString.substring(4, 6));
			zoomSpeed = Integer.parseInt(dataString.substring(6, 8));
			orientDisable = Integer.parseInt(dataString.substring(8, 9));
			invertX = Integer.parseInt(dataString.substring(9, 10));
			invertY = Integer.parseInt(dataString.substring(10, 11));
			invertPitch = Integer.parseInt(dataString.substring(11, 12));
			invertRoll = Integer.parseInt(dataString.substring(12, 13));
		} catch (Exception e) {
			e.printStackTrace();
			
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
		        try { Log.v("LOOK", "port: " + port);
		        	clientSocket = new Socket(IP, Integer.parseInt(port));
		        	outToServer = new PrintWriter(clientSocket.getOutputStream(), true);
		        	inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		        	connected = true;
		        	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		        	Toast.makeText(this, "Connected successfully", Toast.LENGTH_SHORT).show();
		        	connectivity_icon.setImageResource(R.drawable.green_icon);
		        } catch (Exception e){ 
		        	e.printStackTrace();
		        	Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_LONG).show();
		        	connected = false;
		        	shouldBeConnected = false;
		        }
			}
			else
			{
				connected = false;
				Toast.makeText(this, "Please enable Wi-Fi to connect", Toast.LENGTH_LONG).show();
			}
		}
		
		if(!shouldBeConnected && !connected)
		{
			Intent i = new Intent(AccelTest.this, Configure.class);
			startActivityForResult(i, SUB_ACTIVITY_REQUEST_CODE);
		}
	}
	
	/** Called when the application ends */
	protected void onDestroy()
	{ 
		super.onDestroy();
		disconnect(true); 
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
			return true;
		}
		case R.id.configure:
		{
			Intent i = new Intent(AccelTest.this, Configure.class);
			startActivityForResult(i, SUB_ACTIVITY_REQUEST_CODE);
			return true;
		}
		case R.id.connect: //Disconnects from the server 
		{
			disconnect(true);
			return true;
		}
		case R.id.quit:
		{
			quit();
			return true;
		}
		case R.id.about:
		{
			Intent i = new Intent(AccelTest.this, About.class);
			startActivity(i);
			return true;
		}
		case R.id.settings:
		{
			Intent i = new Intent(AccelTest.this, Settings.class);
			startActivity(i);
			return true;
		}
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
			frozen = false;
		}
		else if(requestCode == SUB_ACTIVITY_REQUEST_CODE && resultCode == RESULT_CANCELED && !shouldBeConnected)
		{
			quit();
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
				if(orientDisable == 0)
				{
				xFinal = (x2 - dx);
				if(invertPitch == 0)
					yFinal = (y2 - dy) * pitchSens / 20;
				else
					yFinal = (y2 - dy) * pitchSens / -20;
				if(invertRoll == 0)
					zFinal = (z2 - dz) * rollSens / 20;
				else
					zFinal = (z2 - dz) * rollSens / -20;
				}
				else
				{
					xFinal = 0;
					yFinal = 0;
					zFinal = 0;
				}
				
				if(connected)
				{
					sendValues();
					checkConnection();
				}
			} /*
			if(event.sensor.getType() == TYPE_ACCELEROMETER)
			{
				x1 = event.values[0];
				y1 = event.values[1];
				z1 = event.values[2];
				
				//Sends the output to the server
				if(connected && !frozen)
					outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ); 
				else if(connected && frozen && firstTimeFrozen)
				{
					outToServer.println("0.0,0.0,0.0,0.0,0.0,0.0");
					firstTimeFrozen = false;
				}
			} */
		}
	}
	
	//a method of the View class which needs to be implemented for touch control to work
	public boolean onTouchEvent(MotionEvent me)
	{	
		//Resets scroll values to zero when the user stops touching the screen
		if(me.getAction() == MotionEvent.ACTION_UP || me.getAction() == MotionEvent.ACTION_DOWN)
		{
			distanceX = 0; distanceY = 0;	
			scrolling = false;
		}
		
		if(scrolling == false)
		{
			previousTime = me.getEventTime();
			previousX = me.getRawX();
			previousY = me.getRawY();
			scrolling = true;
		}
		else if(scrolling == true)
		{
			currentTime = me.getEventTime();
			currentX = me.getRawX();
			currentY = me.getRawY();
			diffTime = currentTime - previousTime;
			diffX = currentX - previousX;
			diffY = currentY - previousY;
			
			if(invertX == 0)
				distanceX = diffX / (float) diffTime * panSens;
			else
				distanceX = diffX / (float) diffTime * -panSens;
			if(invertY == 0)
				distanceY = diffY / (float) diffTime * panSens;
			else
				distanceY = diffY / (float) diffTime * -panSens;
			
			previousTime = currentTime;
			previousX = currentX;
			previousY = currentY;
		}
		
		if(connected)
		{
			sendValues();
			checkConnection();
		}
		
		return true;
	}
	
	/** Implemented to allow buttons to be pressed without a complete "onClick" call */
	public boolean onTouch(View v, MotionEvent me)
	{
		if(me.getAction() == MotionEvent.ACTION_UP)
		{
			panZ = 0;
			roll = 0;
		}
		
		if(v == upButton && me.getAction() == MotionEvent.ACTION_DOWN)
			panZ = zoomSpeed;
		else if(v == downButton && me.getAction() == MotionEvent.ACTION_DOWN)
			panZ = zoomSpeed * -1;
		
		if(v == upButton || v == downButton && me.getAction() == MotionEvent.ACTION_DOWN && connected)
		{
			sendValues();
			checkConnection();
		}
		
		return true;
	}
	
	/** Called when an area of the screen is clicked (i.e. a button) */
	public void onClick(View v)
	{
		if(v == freezeButton)
		{
			if(frozen)
			{
				frozen = false;
				
				if(connected)
					connectivity_icon.setImageResource(R.drawable.green_icon);
			}
			else
			{
				frozen = true;
				firstTimeFrozen = true;
				
				if(connected)
					connectivity_icon.setImageResource(R.drawable.yellow_icon);
			}
		}
		
		if(v == calibrateButton)
		{
			dx = x2;
			dy = y2;
			dz = z2;
		}
	}
	
	private void quit()
	{
		onDestroy();
		finish();
	}
	
	private void disconnect(boolean intended)
	{
		shouldBeConnected = false;
		
		if(connected)
		{	
			if(intended == true)
			{
				outToServer.println("0.0,0.0,0.0,0.0,0.0,0.0");
				checkConnection();
			}
			
			outToServer.close();
			
			try {
				clientSocket.close();
				
				if(intended)
					Toast.makeText(this, "Successfully disconnected", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(this, "Lost connection to server", Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			connected = false;
			connectivity_icon.setImageResource(R.drawable.red_icon);
			this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} 
	}
	
	/** Sends the output of sensor values to the server */
	private void sendValues()
	{
		if(connected && !frozen)
			outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ); //roll, pitch, roll, x, y, z
		else if(connected && frozen && firstTimeFrozen)
		{
			outToServer.println("0.0,0.0,0.0,0.0,0.0,0.0");
			firstTimeFrozen = false;
		}
	}
	
	/** Makes sure the app is still connected to the server */
	private void checkConnection()
	{
		if(!frozen || firstTimeFrozen)
		{
			String tmpstr = null;
			
			try {
				tmpstr = inFromServer.readLine();
			} catch (IOException e) {
				disconnect(false);
			}
			
			if(tmpstr == null)
				disconnect(false);
		}
	}
}