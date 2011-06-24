/** Reese Butler
 *  6/24/2011
 */

package com.lg.accel;

import android.app.Activity;
import android.os.Bundle;

public class HelpConfigure extends Activity
{
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_configure); //The sole purpose of this class is to display the contents of help_configure.xml
	}
	
	protected void onPause()
	{
		super.onPause();
		finish();
	}
}
