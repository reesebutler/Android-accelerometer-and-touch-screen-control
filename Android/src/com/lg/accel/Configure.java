/** Reese Butler
 *  8/2/2011
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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

public class Configure extends Activity implements OnClickListener
{
	EditText ip = null;
	EditText port = null;
	EditText enterKey = null;
	EditText timeout = null;
	Button save = null;
	FileOutputStream out = null;
	OutputStreamWriter outWriter = null;
	FileInputStream in = null;
	InputStreamReader inReader = null;
	char[] inputBuffer;
	String s = "", s2 = "", s3 = "", s4 = "";
	boolean finished = false;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure);
		
		//Attempts to retrieve any previously stored IP address
		try{
			inputBuffer = new char[255];
			in = openFileInput("connection.dat");
			inReader = new InputStreamReader(in);
			inReader.read(inputBuffer);
			s = new String(inputBuffer);
			s = s.trim();
			s4 = s.substring(s.indexOf("a") + 1); //Sets the connection timeout
			s3 = s.substring(s.lastIndexOf(",") + 1, s.indexOf("a")); //Sets the passcode
			s2 = s.substring(s.indexOf(",") + 1, s.lastIndexOf(",")); //Sets the port string
			s = s.substring(0, s.indexOf(",")); //Sets the IP string
		} catch (Exception e) {
			e.printStackTrace();
			s = "192.168.1.100";
			s2 = "4444";
			s4 = "7000";
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
		
		//Pre-fills the fields with any existing values
		ip = (EditText) findViewById(R.id.IPtext);
		ip.setText(s);
		port = (EditText) findViewById(R.id.porttext);
		port.setText(s2);
		enterKey = (EditText) findViewById(R.id.keytext);
		enterKey.setText(s3);
		timeout = (EditText) findViewById(R.id.timetext);
		timeout.setText(s4);
		save = (Button) findViewById(R.id.button2);
		save.setOnClickListener(this);
		
		//Only does something if the enter key is pressed
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
		
		//Only does something if the enter key is pressed
		port.setOnKeyListener(new OnKeyListener()
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
		
		//Only does something if the enter key is pressed
		enterKey.setOnKeyListener(new OnKeyListener()
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

		//Only does something if the enter key is pressed
		timeout.setOnKeyListener(new OnKeyListener()
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
	
	/** Creates the menu */
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.config_menu, menu);
	    return true;
	}
	
	/** Determines what to do when the user chooses a menu option */
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()){
		case R.id.help:
		{
			Intent i = new Intent(Configure.this, HelpConfigure.class);
			startActivity(i);
			return true;
		}
		case R.id.about:
		{
			Intent i = new Intent(Configure.this, About.class);
			startActivity(i);
			return true;
		}
		case R.id.quit:
		{
			setResult(RESULT_CANCELED);
			finish();
			return true;
		}
		case R.id.settings:
		{
			Intent i = new Intent(Configure.this, Settings.class);
			startActivity(i);
			return true;
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	//Decides whether or not to save the entered values (based on whether or not they are valid)
	public void onClick(View v)
	{
		boolean ipCorrect = false;
		boolean portCorrect = false;
		boolean keyCorrect = false;
		boolean timeCorrect = false;
		int tmpInt = 0;
		
		if(v == save)
		{	//checks the IP address
			try {
				InetAddress inet = InetAddress.getByName(ip.getText().toString());
				s = inet.toString().substring(1);
				ipCorrect = true;
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Toast.makeText(this, "Not a valid IP address", Toast.LENGTH_SHORT).show();
			}
			
			if(ipCorrect)
			{	//checks the port
				try {
					tmpInt = Integer.parseInt(port.getText().toString());
					s2 = Integer.toString(tmpInt);
					
					if(tmpInt >= 0)
						portCorrect = true;
					else
						Toast.makeText(this, "Not a valid port number", Toast.LENGTH_SHORT).show();
				} catch (NumberFormatException e) {
					e.printStackTrace();
					Toast.makeText(this, "Not a valid port number", Toast.LENGTH_SHORT).show();
				}
			}
			
			//checks the key
			try {
				if(!(enterKey.getText().toString().equals("")))
				{
					tmpInt = Integer.parseInt(enterKey.getText().toString());
					s3 = Integer.toString(tmpInt);
					keyCorrect = true;
				}
				else
				{
					s3 = "";
					keyCorrect = true;
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
				Toast.makeText(this, "Not a valid passcode", Toast.LENGTH_SHORT).show();
			}
			
			//checks the port
			try {
				tmpInt = Integer.parseInt(timeout.getText().toString());
				s4 = Integer.toString(tmpInt);
				
				if(tmpInt >= 0)
					timeCorrect = true;
				else
					Toast.makeText(this, "Not a valid timeout, use a positive value", Toast.LENGTH_SHORT).show();
			} catch (NumberFormatException e) {
				e.printStackTrace();
				Toast.makeText(this, "Not a valid timeout, use an integer", Toast.LENGTH_SHORT).show();
			}
			
			//If all 4 are correct, stores the values and returns to the main screen
			if(ipCorrect && portCorrect && keyCorrect && timeCorrect)
			{
				try {
					out = openFileOutput("connection.dat", MODE_PRIVATE);
					outWriter = new OutputStreamWriter(out);
					outWriter.write(s + "," + s2 + "," + s3 + "a" + s4);
					outWriter.flush();
					setResult(RESULT_OK);
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
}
