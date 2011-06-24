/** Reese Butler
 *  6/24/2011
 */

package com.lg.accel;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;

public class About extends Activity
{
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about); //The sole purpose of this class is to display the contents of help.xml
		Linkify.addLinks((TextView) findViewById(R.id.about), Linkify.ALL);
	}
	
	protected void onPause()
	{
		super.onPause();
		finish();
	}
}