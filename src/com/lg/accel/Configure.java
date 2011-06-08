/** Reese Butler
 *  6/8/2011
 */

package com.lg.accel;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;

public class Configure extends Activity 
{
	EditText ip = null;
	String s;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.configure);
		ip = (EditText) findViewById(R.id.edittext);
		ip.setText("There should be a form here.");
		
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
		//finish();
	}
}
