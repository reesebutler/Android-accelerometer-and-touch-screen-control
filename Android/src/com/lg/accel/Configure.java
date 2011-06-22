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
import android.content.Intent;
import android.os.Bundle;
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

public class Configure extends Activity implements OnClickListener
{
	EditText ip = null;
	EditText port = null;
	Button save = null;
	FileOutputStream out = null;
	OutputStreamWriter outWriter = null;
	FileInputStream in = null;
	InputStreamReader inReader = null;
	char[] inputBuffer = new char[255];
	String s = "", s2 = "";
	boolean finished = false;
	
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
			s2 = s.substring(s.indexOf(",") + 1); //Sets the port string
			s = s.substring(0, s.indexOf(",")); //Sets the IP string
		} catch (Exception e) {
			e.printStackTrace();
			s = "192.168.1.100";
			s2 = "4444";
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
		
		ip = (EditText) findViewById(R.id.IPtext);
		ip.setText(s);
		port = (EditText) findViewById(R.id.porttext);
		port.setText(s2);
		save = (Button) findViewById(R.id.button2);
		save.setOnClickListener(this);
		
		//Strange method that has to be implemented
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
			Intent i = new Intent(Configure.this, Help.class);
			startActivity(i);
			return true;
		}
		case R.id.quit:
		{
			setResult(RESULT_CANCELED);
			finish();
			return true;
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	//Decides whether or not to save the entered IP address (based on whether or not it is valid)
	public void onClick(View v)
	{
		boolean ipCorrect = false;
		boolean portCorrect = false;
		int tmpInt = 0;
		
		if(v == save)
		{
			try {
				InetAddress inet = InetAddress.getByName(ip.getText().toString());
				s = inet.toString().substring(1);
				ipCorrect = true;
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Toast.makeText(this, "Not a valid IP address", Toast.LENGTH_SHORT).show();
			}
			
			if(ipCorrect)
			{
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
			
			if(ipCorrect && portCorrect)
			{
				try {
					out = openFileOutput("settings.dat", MODE_PRIVATE);
					outWriter = new OutputStreamWriter(out);
					outWriter.write(s + "," + s2);
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
