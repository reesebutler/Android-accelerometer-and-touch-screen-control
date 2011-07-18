/** Reese Butler
 *  7/13/2011
 */

package com.lg.accel;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ORIENTATION;
import static android.hardware.SensorManager.SENSOR_DELAY_GAME;
import static android.hardware.SensorManager.SENSOR_DELAY_UI;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
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
* Implements OnClickListener for the buttons
* Implements onTouchListener for the touch-screen output */
public class AccelTest extends Activity implements SensorEventListener, OnClickListener, OnTouchListener
{
	private SensorManager director;
	@SuppressWarnings("unused")
	private float x1 = 0, y1 = 0, z1 = 0, x2 = 0, y2 = 0, z2 = 0, xFinal, yFinal, zFinal, distanceX, distanceY, panZ = 0, roll = 0;
	private float dx = 0, dy = 0, dz = 0;
	private static final int SUB_ACTIVITY_REQUEST_CODE = 100;
	private static String IP = "192.168.1.100", port = "4444", dataString = "";
	private int key = 0;
	private PrintWriter outToServer;
	private BufferedReader inFromServer;
	private Socket clientSocket;
	private WifiManager wifi;
	private FileInputStream in = null;
	private InputStreamReader inReader = null;
	char[] inputBuffer;
	private boolean connected = false, frozen = false, shouldBeConnected = false, firstTimeFrozen = false, autoFrozen = false;
	private Button freezeButton, calibrateButton;
	private ImageButton upButton, downButton;
	private ImageView connectivity_icon;
	private int panSens = 49, pitchSens = 49, rollSens = 49, zoomSpeed = 49, orientDisable = 0, invertX = 0, invertY = 0, invertPitch = 0, invertRoll = 0;
	private Handler handler;
	private AccelTest mainThread;
	private ProgressDialog progress;
	
	//For touch control
	private long previousTime = 0, currentTime, diffTime;
	private float previousX = 0, previousY = 0, currentX, currentY, diffX, diffY, prevDistanceX = 0, prevDistanceY = 0, tmpX, tmpY;
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
        
        handler = new Handler(); //used to help make the app multi-threaded
        mainThread = this; //Gives access to the UI (main) thread
    }

    /** Called when the activity is paused (which happens any time that the user navigates away from the main activity) */
	protected void onPause()
	{
		super.onPause();
		
		//Called so that the sensors do not drain the battery when not in use
		director.unregisterListener(this, director.getDefaultSensor(TYPE_ACCELEROMETER));
		director.unregisterListener(this, director.getDefaultSensor(TYPE_ORIENTATION));
		director = null;
		freeze();
	}
	
	/** Called when the activity is started or resumed */
	protected void onResume()
	{
		super.onResume();
		
		if(autoFrozen)
		{
			frozen = false;
		}
		
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
			inputBuffer = new char[255];
			in = openFileInput("connection.dat");
			inReader = new InputStreamReader(in);
			inReader.read(inputBuffer);
			dataString = new String(inputBuffer);
			dataString = dataString.trim();
			IP = dataString.substring(0, dataString.indexOf(","));
			port = dataString.substring(dataString.indexOf(",") + 1, dataString.lastIndexOf(","));
			
			//Set key equal to the passcode. If none exists, then key is set to 0.
			try {
				key = Integer.parseInt(dataString.substring(dataString.lastIndexOf(",") + 1));
			} catch (NumberFormatException e) {
				key = 0;
			}
		} //If no connection data exists, send the user to the connection screen
		catch (Exception e) {
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
			connect();
		}
		
		if(!shouldBeConnected && !connected)
		{
			Intent i = new Intent(AccelTest.this, Configure.class);
			startActivityForResult(i, SUB_ACTIVITY_REQUEST_CODE);
		}
		
		if(connected)
		{
			if(frozen)
				freezeButton.setText("Unfreeze Output");
			else
				freezeButton.setText("Freeze Output");
		}
		else
			freezeButton.setText("Reconnect");
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
			freeze();
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
		//This method really doesn't need to do anything, but it does need to be implemented for sensorEventListener
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
			tmpX = distanceX;
			tmpY = distanceY;
			
			distanceX = (distanceX + prevDistanceX) / 2;
			distanceY = (distanceY + prevDistanceY) / 2;
			
			prevDistanceX = tmpX;
			prevDistanceY = tmpY;
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
			upButton.setImageResource(R.drawable.upz);
			downButton.setImageResource(R.drawable.downz);
		}
		
		if(v == upButton && me.getAction() == MotionEvent.ACTION_DOWN)
		{
			panZ = zoomSpeed;
			upButton.setImageResource(R.drawable.upz_pressed);
		}
		else if(v == downButton && me.getAction() == MotionEvent.ACTION_DOWN)
		{
			panZ = zoomSpeed * -1;
			downButton.setImageResource(R.drawable.downz_pressed);
		}
		
		if(v == upButton || v == downButton && me.getAction() == MotionEvent.ACTION_DOWN)
		{
			if(connected)
			{
				sendValues();
				checkConnection();
			}
		}
		
		return true;
	}
	
	/** Called when an area of the screen is clicked (i.e. a button) */
	public void onClick(View v)
	{
		if(v == freezeButton)
		{
			if(connected)
			{
				if(frozen)
				{
					frozen = false;
					freezeButton.setText("Freeze Output");
					
					if(connected)
						connectivity_icon.setImageResource(R.drawable.green_icon);
				}
				else
				{
					frozen = true;
					firstTimeFrozen = true;
					freezeButton.setText("Unfreeze Output");
					
					if(connected)
						connectivity_icon.setImageResource(R.drawable.yellow_icon);
				}
			}
			else
			{
				connect();
			}
		}
		
		if(v == calibrateButton)
		{
			dx = x2;
			dy = y2;
			dz = z2;
		}
	}
	
	//Exits the application
	private void quit()
	{
		onDestroy();
		finish();
	}
	
	//Disconnects from the server
	private void disconnect(boolean intended)
	{
		shouldBeConnected = false;
		
		if(connected)
		{	
			if(intended == true)
			{
				outToServer.println("0.0,0.0,0.0,0.0,0.0,0.0" + "," + key);
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
			freezeButton.setText("Reconnect");
		} 
	}
	
	/** Sends the output of sensor values to the server */
	private void sendValues()
	{	
		if(connected && !frozen)
			outToServer.println(roll + "," + yFinal + "," + zFinal + "," + distanceX + "," + distanceY + "," + panZ + "," + key); //roll, pitch, roll, x, y, z
		else if(connected && frozen && firstTimeFrozen)
		{
			outToServer.println("0.0,0.0,0.0,0.0,0.0,0.0" + "," + key);
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
			else if(tmpstr.equals("!code"))
			{
				Toast.makeText(this, "Wrong passcode entered", Toast.LENGTH_SHORT).show();
				disconnect(false);
			}
		}
	}
	
	//Freezes output to the server. First sends 0's for all values to prevent Google Earth from continuing to move.
	private void freeze()
	{
		if(!frozen)
		{
			frozen = true;
			firstTimeFrozen = true;
			autoFrozen = true;
		
			if(connected)
			{
				sendValues();
				checkConnection();
			}
		}
		else
			autoFrozen = false;
	}
	
	/** Attempts to connect to the server */
	private void connect()
	{
		new Thread(new Runnable(){
			public void run()
			{
				if(wifi.isWifiEnabled())
				{
					handler.post(new Runnable() {
						public void run()
						{
							progress = ProgressDialog.show(mainThread, "", "Connecting...");
						}
					});
					
			        try {
			        	clientSocket = new Socket(IP, Integer.parseInt(port));
			        	outToServer = new PrintWriter(clientSocket.getOutputStream(), true);
			        	inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			        	connected = true;
			        	
			        	handler.post(new Runnable() {
			        		public void run()
			        		{
			        			progress.dismiss();
			        			mainThread.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			    	        	Toast.makeText(mainThread, "Connected successfully", Toast.LENGTH_SHORT).show();
			    	        	connectivity_icon.setImageResource(R.drawable.green_icon);
			    	        	freezeButton.setText("Freeze Output");
			        		}
			        	});
			        } catch (Exception e){ 
			        	e.printStackTrace();
			        	
			        	handler.post(new Runnable() {
			        		public void run()
			        		{
			        			progress.dismiss();
			        			Toast.makeText(mainThread, "Failed to connect to server", Toast.LENGTH_LONG).show();
			        		}
			        	});
			        	
			        	connected = false;
			        	shouldBeConnected = false;
			        }
				}
				else
				{
					connected = false;
					
					handler.post(new Runnable() {
						public void run()
						{
							Toast.makeText(mainThread, "Please enable Wi-Fi to connect", Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}
}