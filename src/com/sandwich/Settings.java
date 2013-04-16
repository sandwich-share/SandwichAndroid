package com.sandwich;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;

public class Settings extends Activity implements OnCheckedChangeListener, OnFocusChangeListener {
	private EditText bootstrapText;
	private CheckBox mobileData;
	
	private static final String INITIAL_NODE = "InitialNode";
	private static final String MOBILE_DATA = "MobileData";
	private static final String REFRESH_INTERVAL = "RefreshInterval";
	
	private static final String PREFS_FILE = "SandwichSettings";
	
	private static final String DEFAULT_INITIAL_NODE = "isys-ubuntu.case.edu";
	private static int DEFAULT_REFRESH_INTERVAL = 15 * 1000; // 15 seconds

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		bootstrapText = (EditText) findViewById(R.id.bootstrapText);
		bootstrapText.setOnFocusChangeListener(this);
		mobileData = (CheckBox) findViewById(R.id.mobileDataBox);
		mobileData.setOnCheckedChangeListener(this);
		
		// Restore the saved state from last time we ran
		bootstrapText.setText(getBootstrapNode(this));
		mobileData.setChecked(isMobileDataEnabled(this));
		
		// Check if we have a cell radio
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE)
		{
			// Nope, disable the checkbox
			mobileData.setChecked(false);
			mobileData.setEnabled(false);
		}
	}
	
	public static boolean isMobileDataEnabled(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
		return prefs.getBoolean(MOBILE_DATA, true);
	}
	
	public static String getBootstrapNode(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
		return prefs.getString(INITIAL_NODE, DEFAULT_INITIAL_NODE);
	}
	
	public static int getRefreshInterval(Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
		return prefs.getInt(REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL);
	}
	
	public void updatePreferences()
	{
		SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
		SharedPreferences.Editor editor = prefs.edit();
		
		// Store values from the UI controls
		editor.putString(INITIAL_NODE, bootstrapText.getText().toString());
		editor.putBoolean(MOBILE_DATA, mobileData.isChecked());
		
		// Commit them
		System.out.println("Committing new preferences");
		editor.commit();
	}

	@Override
	public void onFocusChange(View view, boolean focus) {
		if (!focus) {
			// Update preferences when the bootstrap node field looses focus
			updatePreferences();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// Update preferences when checked status changes
		updatePreferences();
	}
}
