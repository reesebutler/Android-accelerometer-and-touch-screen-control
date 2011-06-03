/** Reese Butler
 *  6/3/2011
 */

package com.lg.accel;

import android.app.Activity;
import android.os.Bundle;

public class Help extends Activity
{
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help); //The sole purpose of this class is to display the contents of help.xml
	}
	
	protected void onPause()
	{
		super.onPause();
		finish();
	}
}