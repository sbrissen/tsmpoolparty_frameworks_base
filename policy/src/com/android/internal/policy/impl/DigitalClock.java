package com.android.internal.policy.impl;

/*****************************************************************************************************
/
/  MIUI DigitalClock:
/    
/  Written By: Scott Brissenden
/
*******************************************************************************************************/

import java.util.HashMap;
import android.widget.LinearLayout;
import java.util.Calendar;
import android.widget.ImageView;
import android.content.BroadcastReceiver;
import android.os.Handler;
import android.content.Context;
import android.util.AttributeSet;
import android.text.format.DateFormat;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.util.Log;

import com.android.internal.R;

public class DigitalClock extends LinearLayout{

    private static final String M12 = "h:mm";
    private static final String M24 = "kk:mm";
    private static HashMap<String, Integer> sDigital2ResId = new HashMap<String, Integer>();

    private boolean mAttached;
    private Calendar mCalendar;
    private ImageView mFirstDigital;
    private String mFormat;
    private ImageView mFourthDigital;
    private final Handler mHandler;
    private final BroadcastReceiver mIntentReceiver;
    private ImageView mSecondDigital;
    private ImageView mThirdDigital;

    public DigitalClock(Context context){
	this(context, null);
    }

    public DigitalClock(Context context, AttributeSet attrs){
	super(context,attrs);

	mHandler = new Handler();

	mIntentReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent){
	      if(intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")){
		mCalendar = Calendar.getInstance();
	      }else{
		mHandler.post(new Runnable() {
		    public void run(){
			updateTime();
		    }
		});
	      }
	    }
	 };
    }

    static{
      sDigital2ResId.put("0",Integer.valueOf(R.drawable.lock_screen_bar_time_0));
      sDigital2ResId.put("1",Integer.valueOf(R.drawable.lock_screen_bar_time_1));
      sDigital2ResId.put("2",Integer.valueOf(R.drawable.lock_screen_bar_time_2));
      sDigital2ResId.put("3",Integer.valueOf(R.drawable.lock_screen_bar_time_3));
      sDigital2ResId.put("4",Integer.valueOf(R.drawable.lock_screen_bar_time_4));
      sDigital2ResId.put("5",Integer.valueOf(R.drawable.lock_screen_bar_time_5));
      sDigital2ResId.put("6",Integer.valueOf(R.drawable.lock_screen_bar_time_6));
      sDigital2ResId.put("7",Integer.valueOf(R.drawable.lock_screen_bar_time_7));
      sDigital2ResId.put("8",Integer.valueOf(R.drawable.lock_screen_bar_time_8));
      sDigital2ResId.put("9",Integer.valueOf(R.drawable.lock_screen_bar_time_9));
    }

    private void setDateFormat(){
	if(DateFormat.is24HourFormat(getContext())){
	  mFormat = M24;
	}else{
	  mFormat = M12;
	}
    }

    private void updateTime(){
	mCalendar.setTimeInMillis(System.currentTimeMillis());
	CharSequence newTime = DateFormat.format(mFormat,mCalendar);	
	int start = 0;
	if(newTime.length() == 4){
	  mFirstDigital.setVisibility(View.GONE);
	}else if(newTime.length() == 5){
	  int resId1 = (Integer)(sDigital2ResId.get(String.valueOf(newTime.charAt(0)))).intValue();
	  mFirstDigital.setImageResource(resId1);
	  mFirstDigital.setVisibility(View.VISIBLE);
	  start = 1;
	}
	int resId2 = (Integer)(sDigital2ResId.get(String.valueOf(newTime.charAt(start)))).intValue();
	mSecondDigital.setImageResource(resId2);
	int resId3 = (Integer)(sDigital2ResId.get(String.valueOf(newTime.charAt(start+2)))).intValue();
	mThirdDigital.setImageResource(resId3);
	int resId4 = (Integer)(sDigital2ResId.get(String.valueOf(newTime.charAt(start+3)))).intValue();
	mFourthDigital.setImageResource(resId4);
    }

    protected void onAttachedToWindow(){
	super.onAttachedToWindow();
      
	if(!mAttached){
	    mAttached = true;
	    IntentFilter intent = new IntentFilter();
	    intent.addAction("android.intent.action.TIME_TICK");
	    intent.addAction("android.intent.action.TIME_SET");
	    intent.addAction("android.intent.action.TIMEZONE_CHANGED");
	    mContext.registerReceiver(mIntentReceiver,intent);
	    updateTime();
	}
    }

    protected void onDetachedFromWindow(){
	super.onDetachedFromWindow();

	if(mAttached){
	    mAttached = false;
	    mContext.unregisterReceiver(mIntentReceiver);
	}
    }

    protected void onFinishInflate(){
	super.onFinishInflate();
	
	mFirstDigital = (ImageView) findViewById(R.id.first_digital);
	mSecondDigital = (ImageView) findViewById(R.id.second_digital);
	mThirdDigital = (ImageView) findViewById(R.id.third_digital);
	mFourthDigital = (ImageView) findViewById(R.id.fourth_digital);
	mCalendar = Calendar.getInstance();
	setDateFormat();
    }

    void updateTime(Calendar c){
	mCalendar = c;
	updateTime();
    }
}
	
      

