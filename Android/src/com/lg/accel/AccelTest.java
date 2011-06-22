/** Reese Butler
 *  6/13/2011
 */

package com.lg.accel;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ORIENTATION;
import static android.hardware.SensorManager.SENSOR_DELAY_UI;

import java.io.DataInputStream;
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
	private SensorManager director;
	private GestureDetector detector;
	
	@SuppressWarnings("unused")
	private float x1 = 0, y1 = 0, z1 = 0, x2 = 0, y2 = 0, z2 = 0, xFinal, yFinal, zFinal, distanceX, distanceY, panZ = 0, roll = 0;
	private float dx = 0, dy = 0, dz = 0;
	private static final int SUB_ACTIVITY_REQUEST_CODE = 100;
	private int counter = 0;
	private static String IP = "192.168.1.100", port = "4444", dataString = "";
	private PrintWriter outToServer;
	private DataInputStream inFromServer;
	private Socket clientSocket;
	private WifiManager wifi;
	private FileInputStream in = null;
	private InputStreamReader inReader = null;
	char[] inputBuffer = new char[255];
	private boolean connected = false, frozen = false, shouldBeConnected = false, firstTimeFrozen = false;
	private Button freezeButton, calibrateButton;
	private ImageButton upButton, downButton;
	private ImageView connectivity_icon;
	
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
		        	inFromServer = new DataInputStream(clientSocket.getInputStream());
		        	connected = true;
		        	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		        	Toast.makeText(this, "Connected successfully", Toast.LENGTH_SHORT).show();
		        	connectivity_icon.setImageResource(R.drawable.green_icon);
		        	counter = 0;
		        } catch (Exception e){ 
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
		disconnect(); 
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
		case R.id.connect: //Disconnects from the server 
		{
			disconnect();
		}
			return true;
		case R.id.quit:
		{
			quit();
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
				xFinal = x2 - dx;
				yFinal = y2 - dy;
				zFinal = z2 - dz;
				
				//Sends the output to the server
				if(connected && !frozen)
					outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ); 
				else if(connected && frozen && firstTimeFrozen)
				{
					outToServer.println("0.0,0.0,0.0,0.0,0.0,0.0");
					firstTimeFrozen = false;
				}
				
				//Makes sure the app is still connected to the server
				if(counter % 10 == 0 && connected)
				{
					String tmpstr = null;
					
					try {
						tmpstr = inFromServer.readLine();
					} catch (IOException e) {
						disconnect();
					}
					
					if(tmpstr == null)
						disconnect();
					
					counter = 0;
				}
				
				if(connected)
					counter ++;
			}
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
			}
		}
	}
	
	//a method of the View class which needs to be implemented for touch control to work
	public boolean onTouchEvent(MotionEvent me)
	{
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
		
		if(v == upButton && me.getAction() == MotionEvent.ACTION_DOWN)
			panZ ++;
		else if(v == downButton && me.getAction() == MotionEvent.ACTION_DOWN)
			panZ --;
		
		if(v == upButton || v == downButton && me.getAction() == MotionEvent.ACTION_DOWN)
		{
			//Sends the output to the server
			if(connected && !frozen)
				outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ);
			else if(connected && frozen && firstTimeFrozen)
			{
				outToServer.println("0.0,0.0,0.0,0.0,0.0,0.0");
				firstTimeFrozen = false;
			}
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
		else if(connected && frozen && firstTimeFrozen)
		{
			outToServer.println("0.0,0.0,0.0,0.0,0.0,0.0");
			firstTimeFrozen = false;
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
	
	private void disconnect()
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
			connectivity_icon.setImageResource(R.drawable.red_icon);
			this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} 
	}
}