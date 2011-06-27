/** Reese Butler
 *  6/27/2011
 */

package com.lg.accel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class Settings extends Activity implements OnClickListener
{
	Button save = null, cancel = null;
	FileOutputStream out = null;
	OutputStreamWriter outWriter = null;
	FileInputStream in = null;
	InputStreamReader inReader = null;
	char[] inputBuffer = new char[255];
	String s = "";
	String panSens = "", pitchSens = "", rollSens = "", zoomSpeed = "", orientDisable = "", invertX = "", invertY = "", invertPitch = "", invertRoll = "";
	String panSens1 = "", pitchSens1 = "", rollSens1 = "", zoomSpeed1 = "", orientDisable1 = "", invertX1 = "", invertY1 = "", invertPitch1 = "", invertRoll1 = "";
	boolean finished = false;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		//Attempts to retrieve any previously stored IP address
		try{
			in = openFileInput("settings.dat");
			inReader = new InputStreamReader(in);
			inReader.read(inputBuffer);
			s = new String(inputBuffer);
			s = s.trim();
			
			panSens = s.substring(0, 1); //value between 0 and 8
			pitchSens = s.substring(1, 2); //value between 0 and 8
			rollSens = s.substring(2, 3); //value between 0 and 8
			zoomSpeed = s.substring(3, 4); //value between 0 and 8
			orientDisable = s.substring(4, 5); //value either 0 or 1
			invertX = s.substring(5, 6); //value either 0 or 1
			invertY = s.substring(6, 7); //value either 0 or 1
			invertPitch = s.substring(7, 8); //value either 0 or 1
			invertRoll = s.substring(8, 9); //value either 0 or 1
		} catch (Exception e) {
			panSens = "4";
			pitchSens = "4";
			rollSens = "4";
			zoomSpeed = "4";
			orientDisable = "0";
			invertX = "0";
			invertY = "0";
			invertPitch = "0";
			invertRoll = "0";
			Toast.makeText(this, "Unable to read previous settings", Toast.LENGTH_SHORT).show();
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
		
		save = (Button) findViewById(R.id.button2);
		cancel = (Button) findViewById(R.id.button3);
		save.setOnClickListener(this);
		cancel.setOnClickListener(this);
	}
	
	protected void onPause()
	{
		super.onPause();
	}

	//Decides whether or not to save the entered IP address (based on whether or not it is valid)
	public void onClick(View v)
	{
		
		if(v == save)
		{
			try {
				out = openFileOutput("settings.dat", MODE_PRIVATE);
				outWriter = new OutputStreamWriter(out);
				outWriter.write(panSens1 + pitchSens1 + rollSens1 + zoomSpeed1 + orientDisable1 + invertX1 + invertY1 + invertPitch1 + invertRoll1);
				outWriter.flush();
				finish();
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, "Data not saved", Toast.LENGTH_SHORT).show();
			} finally {
				try {
					outWriter.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else if(v == cancel)
		{
			try {
				out = openFileOutput("settings.dat", MODE_PRIVATE);
				outWriter = new OutputStreamWriter(out);
				outWriter.write(panSens + pitchSens + rollSens + zoomSpeed + orientDisable + invertX + invertY + invertPitch + invertRoll);
				outWriter.flush();
				finish();
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, "Data not saved", Toast.LENGTH_SHORT).show();
			} finally {
				try {
					outWriter.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}