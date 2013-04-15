package com.sandwich;

import android.os.Bundle;
import android.app.Activity;
import android.widget.CheckBox;
import android.widget.EditText;

public class Settings extends Activity {
	EditText bootstrapText;
	CheckBox mobileData;
	
	private static final String INITIAL_NODE = "InitialNode";
	private static final String MOBILE_DATA = "MobileData";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		bootstrapText = (EditText) findViewById(R.id.bootstrapText);
		mobileData = (CheckBox) findViewById(R.id.mobileDataBox);
		
		// Restore the saved state from last time we ran
		if (savedInstanceState != null) {
			System.out.println("Restored settings");
			if (savedInstanceState.containsKey(INITIAL_NODE)) {
				bootstrapText.setText(savedInstanceState.getString(INITIAL_NODE));
			}
			if (savedInstanceState.containsKey(MOBILE_DATA)) {
				mobileData.setChecked(savedInstanceState.getBoolean(MOBILE_DATA));
			}
		} else {
			System.out.println("Initialized settings with defaults");
		}
	}
}
