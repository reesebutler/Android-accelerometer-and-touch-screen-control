/** Reese Butler
 *  7/5/2011
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
	EditText enterKey = null;
	Button save = null;
	FileOutputStream out = null;
	OutputStreamWriter outWriter = null;
	FileInputStream in = null;
	InputStreamReader inReader = null;
	char[] inputBuffer;
	String s = "", s2 = "", s3 = "";
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
			s3 = s.substring(s.lastIndexOf(",") + 1);
			s2 = s.substring(s.indexOf(",") + 1, s.lastIndexOf(",")); //Sets the port string
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
		enterKey = (EditText) findViewById(R.id.keytext);
		enterKey.setText(s3);
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
	
	//Decides whether or not to save the entered IP address (based on whether or not it is valid)
	public void onClick(View v)
	{
		boolean ipCorrect = false;
		boolean portCorrect = false;
		boolean keyCorrect = false;
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
			
			if(ipCorrect && portCorrect && keyCorrect)
			{
				try {
					out = openFileOutput("connection.dat", MODE_PRIVATE);
					outWriter = new OutputStreamWriter(out);
					outWriter.write(s + "," + s2 + "," + s3);
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
