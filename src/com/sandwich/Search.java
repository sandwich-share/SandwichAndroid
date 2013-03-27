package com.sandwich;

import com.sandwich.R;

import android.os.Bundle;
import android.app.Activity;

public class Search extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        new Thread(new JankyThread(this)).start();
    }

    @Override
    protected void onUserLeaveHint()
    {
    	super.onUserLeaveHint();
    	
    	finish();
    }
    
    @Override
    protected void onDestroy()
    {
    	super.onDestroy();
    	
    	Dialog.closeDialogs();
    	SpinnerDialog.closeDialogs();
    }
}
