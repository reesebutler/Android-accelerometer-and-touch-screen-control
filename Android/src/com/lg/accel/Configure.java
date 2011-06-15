/** Reese Butler
 *  6/9/2011
 */

package com.lg.accel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Configure extends Activity implements OnClickListener
{
	EditText ip = null;
	Button save = null;
	FileOutputStream out = null;
	OutputStreamWriter outWriter = null;
	FileInputStream in = null;
	InputStreamReader inReader = null;
	char[] inputBuffer = new char[255];
	String s = "";
	boolean finished = false;
	//private static final String TAG = "MyActivity"; //For debugging purposes
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure);
		
		//Attempts to retrieve any previously stored IP address
		try{
			in = openFileInput("settings.dat");
			inReader = new InputStreamReader(in);
			inReader.read(inputBuffer);
			s = new String(inputBuffer);
			s = s.trim();
		} catch (Exception e) {
			e.printStackTrace();
			s = "192.168.1.100";
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
		
		ip = (EditText) findViewById(R.id.edittext);
		ip.setText(s);
		save = (Button) findViewById(R.id.button2);
		save.setOnClickListener(this);
		
		ip.setOnKeyListener(new OnKeyListener()
		{
		    public boolean onKey(View v, int keyCode, KeyEvent event) 
		    {
		    	if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER))
		    	{
		    		return true;
		    	}
		    	return false;
		    }
		});

	}
	
	protected void onPause()
	{
		super.onPause();
	}
	
	//Decides whether or not to save the entered IP address (based on whether or not it is valid)
	public void onClick(View v)
	{
		boolean correct = false;
		
		if(v == save)
		{
			try {
				InetAddress inet = InetAddress.getByName(ip.getText().toString());
				s = inet.toString().substring(1);
				correct = true;
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Toast.makeText(this, "Not a valid IP address", Toast.LENGTH_SHORT).show();
			}
			
			if(correct)
			{
				try {
					out = openFileOutput("settings.dat", MODE_PRIVATE);
					outWriter = new OutputStreamWriter(out);
					outWriter.write(s);
					outWriter.flush();
					Toast.makeText(this, "Settings successfully saved", Toast.LENGTH_SHORT).show();
					finished = true;
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(this, "Settings not saved", Toast.LENGTH_SHORT).show();
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
		
		if(finished)
			finish();
	}
}
