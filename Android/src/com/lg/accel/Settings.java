/** Reese Butler
 *  6/28/2011
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
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends Activity implements OnClickListener, OnSeekBarChangeListener
{
	Button save = null, cancel = null, reset = null;
	FileOutputStream out = null;
	OutputStreamWriter outWriter = null;
	FileInputStream in = null;
	InputStreamReader inReader = null;
	char[] inputBuffer = new char[255];
	String s = "";
	String panSens = "", pitchSens = "", rollSens = "", zoomSpeed = "", orientDisable = "", invertX = "", invertY = "", invertPitch = "", invertRoll = "";
	String panSens1 = "", pitchSens1 = "", rollSens1 = "", zoomSpeed1 = "", orientDisable1 = "", invertX1 = "", invertY1 = "", invertPitch1 = "", invertRoll1 = "";
	boolean finished = false;
	TextView text1, text2, text3, text4;
	SeekBar seek1, seek2, seek3, seek4;
	CheckBox box1, box2, box3, box4, box5;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		//Attempts to retrieve any previously stored settings
		try{
			in = openFileInput("settings.dat");
			inReader = new InputStreamReader(in);
			inReader.read(inputBuffer);
			s = new String(inputBuffer);
			s = s.trim();
			
			panSens = s.substring(0, 2); //value between 0 and 99
			pitchSens = s.substring(2, 4); //value between 0 and 99
			rollSens = s.substring(4, 6); //value between 0 and 99
			zoomSpeed = s.substring(6, 8); //value between 0 and 99
			orientDisable = s.substring(8, 9); //value either 0 or 1
			invertX = s.substring(9, 10); //value either 0 or 1
			invertY = s.substring(10, 11); //value either 0 or 1
			invertPitch = s.substring(11, 12); //value either 0 or 1
			invertRoll = s.substring(12, 13); //value either 0 or 1
		} catch (Exception e) {
			panSens = "49";
			pitchSens = "49";
			rollSens = "49";
			zoomSpeed = "49";
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
		
		panSens1 = panSens;
		pitchSens1 = pitchSens;
		rollSens1 = rollSens;
		zoomSpeed1 = zoomSpeed;
		orientDisable1 = orientDisable;
		invertX1 = invertX;
		invertY1 = invertY;
		invertPitch1 = invertPitch;
		invertRoll1 = invertRoll;
		
		text1 = (TextView) findViewById(R.id.text1);
		text2 = (TextView) findViewById(R.id.text2);
		text3 = (TextView) findViewById(R.id.text3);
		text4 = (TextView) findViewById(R.id.text4);
		seek1 = (SeekBar) findViewById(R.id.seek1);
		seek2 = (SeekBar) findViewById(R.id.seek2);
		seek3 = (SeekBar) findViewById(R.id.seek3);
		seek4 = (SeekBar) findViewById(R.id.seek4);
		box1 = (CheckBox) findViewById(R.id.box1);
		box2 = (CheckBox) findViewById(R.id.box2);
		box3 = (CheckBox) findViewById(R.id.box3);
		box4 = (CheckBox) findViewById(R.id.box4);
		box5 = (CheckBox) findViewById(R.id.box5);
		
		box1.setOnClickListener(this);
		box2.setOnClickListener(this);
		box3.setOnClickListener(this);
		box4.setOnClickListener(this);
		box5.setOnClickListener(this);
		
		/** Initializes the text to the values of the seek bars */
		text1.setText("Set panning sensitivity: " + (Integer.parseInt(panSens) + 1) + "%");
		text2.setText("Set pitch sensitivity: " + (Integer.parseInt(pitchSens) + 1) + "%");
		text3.setText("Set roll sensitivity: " + (Integer.parseInt(rollSens) + 1) + "%");
		text4.setText("Set zoom sensitivity: " + (Integer.parseInt(zoomSpeed) + 1) + "%");
		
		/** Initializes the seek bars to the previous values */
		seek1.setProgress(Integer.parseInt(panSens));
		seek2.setProgress(Integer.parseInt(pitchSens));
		seek3.setProgress(Integer.parseInt(rollSens));
		seek4.setProgress(Integer.parseInt(zoomSpeed));

		/** Checks the checkboxes if necessary */
		if(orientDisable.equals("1"))
			box1.setChecked(true);
		if(invertX.equals("1"))
			box2.setChecked(true);
		if(invertY.equals("1"))
			box3.setChecked(true);
		if(invertPitch.equals("1"))
			box4.setChecked(true);
		if(invertRoll.equals("1"))
			box5.setChecked(true);
		
		seek1.setOnSeekBarChangeListener(this);
		seek2.setOnSeekBarChangeListener(this);
		seek3.setOnSeekBarChangeListener(this);
		seek4.setOnSeekBarChangeListener(this);
		
		save = (Button) findViewById(R.id.button2);
		cancel = (Button) findViewById(R.id.button3);
		reset = (Button) findViewById(R.id.button4);
		save.setOnClickListener(this);
		cancel.setOnClickListener(this);
		reset.setOnClickListener(this);
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
				Toast.makeText(this, "Settings successfully saved", Toast.LENGTH_SHORT).show();
				finish();
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, "Unable to save data", Toast.LENGTH_SHORT).show();
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
			finish();
		}
		else if(v == reset)
		{
			panSens1 = "49";
			pitchSens1 = "49";
			rollSens1 = "49";
			zoomSpeed1 = "49";
			orientDisable1 = "0";
			invertX1 = "0";
			invertY1 = "0";
			invertPitch1 = "0";
			invertRoll1 = "0";
			
			box1.setChecked(false);
			box2.setChecked(false);
			box3.setChecked(false);
			box4.setChecked(false);
			box5.setChecked(false);
			
			seek1.setProgress(49);
			seek2.setProgress(49);
			seek3.setProgress(49);
			seek4.setProgress(49);
		}
		
		/** Handles the states of the checkboxes */
		if(v == box1)
		{
			if(((CheckBox)v).isChecked())
				orientDisable1 = "1";
			else
				orientDisable1 = "0";
		}
		else if(v == box2)
		{
			if(((CheckBox)v).isChecked())
				invertX1 = "1";
			else
				invertX1 = "0";
		}
		else if(v == box3)
		{
			if(((CheckBox)v).isChecked())
				invertY1 = "1";
			else
				invertY1 = "0";
		}
		else if(v == box4)
		{
			if(((CheckBox)v).isChecked())
				invertPitch1 = "1";
			else
				invertPitch1 = "0";
		}
		else if(v == box5)
		{	
			if(((CheckBox)v).isChecked())
				invertRoll1 = "1";
			else
				invertRoll1 = "0";
		}
	}
	
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch)
	{
		if(seekBar == seek1)
		{
			if(progress < 10)
				panSens1 = "0" + progress;
			else
				panSens1 = "" + progress;
			
			text1.setText("Set panning sensitivity: " + (progress + 1) + "%");
		}
		if(seekBar == seek2)
		{
			if(progress < 10)
				pitchSens1 = "0" + progress;
			else
				pitchSens1 = "" + progress;
			
			text2.setText("Set pitch sensitivity: " + (progress + 1) + "%");
		}
		if(seekBar == seek3)
		{
			if(progress < 10)
				rollSens1 = "0" + progress;
			else
				rollSens1 = "" + progress;
			text3.setText("Set roll sensitivity: " + (progress + 1) + "%");
		}
		if(seekBar == seek4)
		{
			if(progress < 10)
				zoomSpeed1 = "0" + progress;
			else
				zoomSpeed1 = "" + progress;
			text4.setText("Set zoom sensitivity: " + (progress + 1) + "%");
		}
	}
	
	public void onStartTrackingTouch(SeekBar seekBar){}
	public void onStopTrackingTouch(SeekBar seekBar){}
}