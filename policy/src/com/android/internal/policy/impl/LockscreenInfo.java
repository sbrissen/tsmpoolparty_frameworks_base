/*****************************************************************************************************
/
/
/  Lockscreen Information Widget:
/    -Displays Weather, Battery Info, and Alarm Info on Lockscreen
     -REQUIRES: AccuWeatherDaemonService.apk
/
/  This was modeled from the SGS2 Weather widget
/
/  Converted/Written By: Scott Brissenden
*******************************************************************************************************/

package com.android.internal.policy.impl;

import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.content.BroadcastReceiver;
import android.graphics.drawable.*;
import android.widget.*;
import android.os.Handler;
import android.os.Message;
import android.content.Intent;
import com.android.internal.policy.impl.KeyguardUpdateMonitor;
import com.android.internal.widget.LockPatternUtils;
import android.content.res.Configuration;
import android.content.Context;
import android.view.LayoutInflater;
import android.content.ContentResolver;
import android.provider.Settings;
import android.view.ViewGroup;
import android.content.IntentFilter;
import android.util.Log;
import android.content.res.Resources;

import com.android.internal.R;

public class LockscreenInfo extends LinearLayout {

  public static final String ACTION_SEC_CHANGE_SETTING = "com.sec.android.widgetapp.accuweatherdaemon.action.CHANGE_SETTING";
  public static final String ACTION_SEC_CHANGE_WEATHER_DATA = "com.sec.android.widgetapp.accuweatherdaemon.action.CHANGE_WEATHER_DATA";
  public static final int DAEMON_TURN_OFF = 0x0;
  public static final int DAEMON_TURN_ON = 0x1;
  public static final int DEFAULT = 0x2;
  private static final boolean DEBUG = true;
  public static final String KEY_AUTO_REFRESH_INTERVAL = "aw_daemon_service_key_autorefresh_interval";
  public static final String KEY_CITY_NAME = "CITY_NAME";
  public static final String KEY_CURRENT_TEMP = "aw_daemon_service_key_current_temp";
  public static final String KEY_DAEMON_ON_OFF = "aw_daemon_service_key_service_status";
  public static final String KEY_ICON_NUM = "aw_daemon_service_key_icon_num";
  public static final String KEY_TEMP_SCALE = "aw_daemon_service_key_temp_scale";
  public static final String KEY_UPDATED_TIME = "aw_daemon_service_key_updated_time";
  public static final String KEY_HIGH_TEMP = "aw_daemon_service_key_high_temp";
  public static final String KEY_LOW_TEMP = "aw_daemon_service_key_low_temp";
  public static final String KEY_WEATHER_TEXT = "aw_daemon_service_key_weather_text";
  private static final int MSG_BOOT_COMPLETED = 0x140;
  private static final int MSG_WEATHER_SETTING_CHANGED = 0x12c;
  private static final int MSG_WEATHER_UPDATED = 0x136;
  private static final String TAG = "LockInfoWidget";
  public static final int TEMP_SCALE_CENTIGRADE = 0x1;
  public static final int TEMP_SCALE_FAHRENHEIT = 0x0;

  private int mBatteryLevel;
  private boolean mBootCompleted;
  private BroadcastReceiver mBroadcastReceiver;
  private String mCharging;
  private Drawable mChargingIcon;
  private RelativeLayout mChargingLayout;
  private TextView mChargingText;
  private Handler mHandler;
  private int mLayoutPosition;
  private boolean mPluggedIn;
  private boolean mShowingBatteryInfo;
  private LinearLayout mSpace_01;
  private LinearLayout mSpace_02;
  private Drawable mTemperatureUnit;
  private KeyguardUpdateMonitor mUpdateMonitor;
  private TextView mWeatherText;
  private TextView mTempHi;
  private TextView mTempLo;
  private int mWeatherDaemonState;
  private ImageView mWeatherIcon;
  private RelativeLayout mWeatherLayout;
  private TextView mWeatherTemperature;
  private Context mContext;
  private String mNextAlarm;
  private Drawable mAlarmIcon;
  private TextView mAlarmText;
  private LockPatternUtils mLockPatternUtils;

  private boolean mLockAlwaysBattery = (Settings.System.getInt(getContext().getContentResolver(),
            Settings.System.LOCKSCREEN_BATTERY_INFO, 0) == 1);

  private boolean mShowingInfo = (Settings.System.getInt(getContext().getContentResolver(),
            Settings.System.LOCKSCREEN_SHOW_INFO, 0) == 1);

  public LockscreenInfo(Context context,KeyguardUpdateMonitor updateMonitor,Configuration configuration, LockPatternUtils lockPatternUtils){
    super(context);

    mBatteryLevel = 0x64;
    mLayoutPosition = 0x2;
    mLockPatternUtils = lockPatternUtils;

    LayoutInflater inflater = LayoutInflater.from(context);

    inflater.inflate(R.layout.keyguard_screen_infowidget,this,true);
    mLayoutPosition = (Settings.System.getInt(context.getContentResolver(),"clock_position", 2));

    mWeatherLayout = (RelativeLayout) findViewById(R.id.info_weather);
    mChargingLayout = (RelativeLayout) findViewById(R.id.info_charging);
    
    mChargingLayout.setVisibility(0x0);
    mSpace_01 = (LinearLayout) findViewById(R.id.temp_clockspace_01);
    mSpace_02 = (LinearLayout) findViewById(R.id.temp_clockspace_02);
    mSpace_01.setVisibility(0x8);
    mSpace_02.setVisibility(0x8);
    mAlarmText = (TextView) findViewById(R.id.alarm_text);
    mWeatherText = (TextView) findViewById(R.id.weather_text);
    mWeatherTemperature = (TextView) findViewById(R.id.weather_temp);
    mWeatherIcon = (ImageView) findViewById(R.id.weather_icon);
    mChargingText = (TextView) findViewById(R.id.charging_text);
	mTempHi = (TextView) findViewById(R.id.weather_hi);
	mTempLo = (TextView) findViewById(R.id.weather_lo);

    mHandler = new Handler(){
	public void handleMessage(Message msg){
	  switch(msg.what) {
	    case MSG_WEATHER_SETTING_CHANGED:
      	      Log.d("InfoWidget","Settings Changed!");
	      handleChangeWeatherSetting(0x1);
	    case MSG_WEATHER_UPDATED:
	      Log.d("InfoWidget","Weather Updated!");
	      handleUpdateWeather();
	    case MSG_BOOT_COMPLETED:
	      Log.d("InfoWidget","Boot Completed");
	      handleBootCompleted();
	  }
	}
    };

    mUpdateMonitor = updateMonitor;

    IntentFilter filter = new IntentFilter();
    filter.addAction("com.sec.android.widgetapp.accuweatherdaemon.action.CHANGE_WEATHER_DATA");
    filter.addAction("android.intent.action.BOOT_COMPLETED");
    
    mBroadcastReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent){
	String action = intent.getAction();

	if(action.equals("com.sec.android.widgetapp.accuweatherdaemon.action.CHANGE_SETTING")){
	  int service_status = intent.getIntExtra("aw_daemon_service_key_service_status", 0x0);
	  mHandler.sendMessage(mHandler.obtainMessage(0x12c,service_status,0x64));
	}else if(action.equals("com.sec.android.widgetapp.accuweatherdaemon.action.CHANGE_WEATHER_DATA")){
	  mHandler.sendMessage(mHandler.obtainMessage(0x136));
	}else if(action.equals("android.intent.action.BOOT_COMPLETED")){
	  mHandler.sendMessage(mHandler.obtainMessage(0x140));
	}
      }
    };

    context.registerReceiver(mBroadcastReceiver,filter);

    init();
  }

  private int findDrawableId(int weatherIconNum){
    switch(weatherIconNum) {
      case 0x0:
	return 0x0;
      case 0x1:
	return 0x1;
      case 0x2:
	return 0x2;
      case 0x4:
	return 0x4;
      case 0x5:
	return 0x5;
      case 0x6:
	return 0x6;
      case 0x7:
	return 0x7;
      case 0x8:
	return 0x8;
      case 0x9:
	return 0x9;
      case 0xa:
	return 0xa;
      case 0xb:
	return 0xb;
      case 0xc:
	return 0xc;
      case 0xd:
	return 0xd;
      case 0xe:
	return 0xe;
      case 0xf:
	return 0xf;
      case 0x10:
	return 0x10;
      case 0x11:
	return 0x11;
      case 0x12:
	return 0x12;
      case 0x13:
	return 0x13;
    }
    return 0x0;
  }

  private void handleChangeWeatherSetting(int daemonState){
    mWeatherDaemonState = daemonState;
    setInfoTempLayout(daemonState);
    updateChargingInfo();
    refreshAlarmDisplay();
    setWeatherInfoVisibility(daemonState);
  }

  private void handleUpdateWeather(){
    updateWeatherInfo();
    Log.d("InfoWidget","handleUpdateWeather");
  }

  private void handleBootCompleted(){
    mBootCompleted = true;
  }

  private void init(){
    Log.d("InfoWidget","init()");
	handleChangeWeatherSetting(0x1);
	handleUpdateWeather();    
  }

    private void refreshBatteryStringAndIcon() {
        if (!mShowingBatteryInfo && !mLockAlwaysBattery) {
            mCharging = null;
            return;
        }

        if (mPluggedIn) {
            mChargingIcon =
                getContext().getResources().getDrawable(R.drawable.ic_lock_idle_charging);
            if (mUpdateMonitor.isDeviceCharged()) {
                mCharging = getContext().getString(R.string.lockscreen_charged);
            } else {
                mCharging = getContext().getString(R.string.lockscreen_plugged_in, mBatteryLevel);
            }
        } else {
            if (mBatteryLevel <= 20) {
                mChargingIcon =
                    getContext().getResources().getDrawable(R.drawable.ic_lock_idle_low_battery);
                mCharging = getContext().getString(R.string.lockscreen_low_battery, mBatteryLevel);
            } else {
                mChargingIcon =
                    getContext().getResources().getDrawable(R.drawable.ic_lock_idle_discharging);
                mCharging = getContext().getString(R.string.lockscreen_discharging, mBatteryLevel);
            }
        }
    }

    private void refreshAlarmDisplay() {
        String mNextAlarm = mLockPatternUtils.getNextAlarm();
        if (mNextAlarm != null) {
            mAlarmIcon = getContext().getResources().getDrawable(R.drawable.ic_lock_idle_alarm);
      mAlarmText.setText(mNextAlarm);
      mAlarmText.setCompoundDrawablesWithIntrinsicBounds(mAlarmIcon,null,null,null);
        }        
    }

  private void setInfoTempLayout(int daemonState){
    switch(daemonState){
      case DEFAULT:
	Log.d("InfoWidget","setInfoTempLayout() Daemon state is null");
	mSpace_01.setVisibility(0x0);
	mSpace_02.setVisibility(0x0);
      case DAEMON_TURN_ON:
	if(mLayoutPosition == 0x2 && mCharging != null){
	  mSpace_01.setVisibility(0x0);
	  mSpace_02.setVisibility(0x0);
	}else if(mLayoutPosition == 0x2 && mCharging == null){
	  mSpace_01.setVisibility(0x8);
	  mSpace_02.setVisibility(0x0);
	}else if(mLayoutPosition == 0x1 && mCharging != null){
  	  mSpace_01.setVisibility(0x0);
	  mSpace_02.setVisibility(0x8);
	}else{
  	  mSpace_01.setVisibility(0x8);
	  mSpace_02.setVisibility(0x8);	 
	}	  
      case DAEMON_TURN_OFF:
	if(mLayoutPosition == 0x2 && mCharging != null){
  	  mSpace_01.setVisibility(0x0);
	  mSpace_02.setVisibility(0x8);
	}else if(mLayoutPosition == 0x1 && mCharging != null){
  	  mSpace_01.setVisibility(0x0);
	  mSpace_02.setVisibility(0x8);	  
	}else{
    	  mSpace_01.setVisibility(0x8);
	  mSpace_02.setVisibility(0x8);	
	}
    }
  }

  private void setWeatherIcon(int iconNum){
    int[] unlock_weather_drawables = {
	R.drawable.weather_2_01_02,
	R.drawable.weather_2_01_02,
	R.drawable.weather_2_03_04_05,
	R.drawable.weather_2_03_04_05,
	R.drawable.weather_2_03_04_05,
	R.drawable.weather_2_06_07_08,
	R.drawable.weather_2_06_07_08,
	R.drawable.weather_2_06_07_08,
	R.drawable.weather_2_11,
	R.drawable.weather_2_12_13_39_40,
	R.drawable.weather_2_14,
	R.drawable.weather_2_15_41_42,
	R.drawable.weather_2_16_17,
	R.drawable.weather_2_16_17,
	R.drawable.weather_2_18,
	R.drawable.weather_2_19_43,
	R.drawable.weather_2_20_21,
	R.drawable.weather_2_20_21,
	R.drawable.weather_2_22_23_44,
	R.drawable.weather_2_22_23_44,
	R.drawable.weather_2_24_25_26,
	R.drawable.weather_2_24_25_26,
	R.drawable.weather_2_24_25_26,
	R.drawable.weather_2_29,
	R.drawable.weather_2_30,
	R.drawable.weather_2_31,
	R.drawable.weather_2_32,
	R.drawable.weather_2_33,
	R.drawable.weather_2_34_35_36_37,
	R.drawable.weather_2_34_35_36_37,
	R.drawable.weather_2_34_35_36_37,
	R.drawable.weather_2_34_35_36_37,
	R.drawable.weather_2_38,
	R.drawable.weather_2_12_13_39_40,
	R.drawable.weather_2_12_13_39_40,
	R.drawable.weather_2_15_41_42,
	R.drawable.weather_2_15_41_42,
	R.drawable.weather_2_19_43,
	R.drawable.weather_2_22_23_44
    };

    int i = findDrawableId(iconNum);
    mWeatherIcon.setImageResource(unlock_weather_drawables[i]);
  }

  private void setWeatherInfoVisibility(int daemonState){
    switch(daemonState){
      case DEFAULT:
	mWeatherLayout.setVisibility(0x8);
      case DAEMON_TURN_OFF:
	mWeatherLayout.setVisibility(0x8);
      case DAEMON_TURN_ON:
	updateWeatherInfo();
	mWeatherLayout.setVisibility(0x0);
    }
  }

  private void updateChargingStatus(){
    if(mCharging == null){
      Log.d("InfoWidget","mCharging is null");
      mChargingText.setVisibility(0x8);
    }else if(mCharging != null){
      mChargingText.setVisibility(0x0);
      mChargingText.setText(mCharging);
      mChargingText.setCompoundDrawablesWithIntrinsicBounds(mChargingIcon,null,null,null);
    }
  }

  private void updateWeatherView(float currentTemp, int tempScale, int iconNum, String weatherTxt, String tHigh, String tLow){
    mWeatherText.setText(weatherTxt);
	
    int mScale = (Settings.System.getInt(getContext().getContentResolver(),Settings.System.KEY_TEMP_SCALE,0x1));
    if(mScale == 0x1){ 		
      mTemperatureUnit = getContext().getResources().getDrawable(R.drawable.unlock_tem_c);
    }else if(mScale == 0x0){		
      mTemperatureUnit = getContext().getResources().getDrawable(R.drawable.unlock_tem_f);
    }else{
      mTemperatureUnit = getContext().getResources().getDrawable(R.drawable.unlock_tem_c);
    }
    
    String text_currentTemp = Float.toString(currentTemp);
    mWeatherTemperature.setText(text_currentTemp);
    mWeatherTemperature.setCompoundDrawablesWithIntrinsicBounds(null,null,mTemperatureUnit,null);
	mTempHi.setText(tHigh);
	mTempHi.setCompoundDrawablesWithIntrinsicBounds(null,null,mTemperatureUnit,null);
	mTempLo.setText(tLow);
	mTempLo.setCompoundDrawablesWithIntrinsicBounds(null,null,mTemperatureUnit,null);
  }

  public void cleanUp(){
    getContext().unregisterReceiver(mBroadcastReceiver);
    Log.d("InfoWidget","cleanUp()");
  }

  public void onPause(){
    Log.d("InfoWidget","onPause()");
  }

  public void onResume(){
    Log.d("InfoWidget","onResume()");
    updateChargingInfo();
    refreshAlarmDisplay();
    setWeatherInfoVisibility(mWeatherDaemonState);
    setInfoTempLayout(mWeatherDaemonState);
  }

  public void updateChargingInfo(){
    mShowingBatteryInfo = mUpdateMonitor.shouldShowBatteryInfo();
    mPluggedIn = mUpdateMonitor.isDevicePluggedIn();
    mBatteryLevel = mUpdateMonitor.getBatteryLevel();
    refreshBatteryStringAndIcon();
    updateChargingStatus();
  }

  public void updateWeatherInfo(){    
    String weatherText = Settings.System.getString(getContext().getContentResolver(),Settings.System.KEY_WEATHER_TEXT);
    String updateDate = Settings.System.getString(getContext().getContentResolver(),Settings.System.KEY_UPDATED_TIME);
    float currentTemp = (Settings.System.getFloat(getContext().getContentResolver(),Settings.System.KEY_CURRENT_TEMP,0x0));
    int tempScale = (Settings.System.getInt(getContext().getContentResolver(),Settings.System.KEY_TEMP_SCALE,0x1));
    int iconNum = (Settings.System.getInt(getContext().getContentResolver(),Settings.System.KEY_ICON_NUM,0x0));
    int daemonStatus = (Settings.System.getInt(getContext().getContentResolver(),Settings.System.KEY_DAEMON_ON_OFF,0x0));
    int refreshTime = (Settings.System.getInt(getContext().getContentResolver(),Settings.System.KEY_AUTO_REFRESH_INTERVAL, 0x0));
	float tmpHi = (Settings.System.getFloat(getContext().getContentResolver(),Settings.System.KEY_HIGH_TEMP,0x0));
	float tmpLo = (Settings.System.getFloat(getContext().getContentResolver(),Settings.System.KEY_LOW_TEMP,0x0));
	String tempHi = new StringBuilder().append("Hi: ").append(tmpHi).toString();
	String tempLo = new StringBuilder().append("Lo: ").append(tmpLo).toString();
	setWeatherIcon(iconNum);
    updateWeatherView(currentTemp,tempScale,iconNum,weatherText,tempHi,tempLo);
    refreshAlarmDisplay();
  }
}

    