/*****************************************************************************************************
/
/
/	Lockscreen Wallpaper Updater:
/		-Displays a user selected wallpaper (via TSMParts) on Lockscreen
/
/	This was modeled from the SGS2 Wallpaper Updater
/
/	Converted/Written By: Scott Brissenden
*******************************************************************************************************/

package com.android.internal.policy.impl;

import android.content.BroadcastReceiver;
import android.os.Handler;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;
import android.widget.RelativeLayout;
import android.util.Log;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.*;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.LayoutInflater;
import java.io.File;
import android.view.ViewGroup;
import android.provider.Settings;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;
import java.io.InputStream;
import java.io.FileInputStream;


import com.android.internal.R;

class LockscreenWallpaperUpdater extends RelativeLayout {
    
    private static final boolean DEBUG = true;
    private static final int IS_CHANGED_DRAWABLE = 1;
    private static final int IS_NOT_CHANGED_DRAWABLE = 0;
    private static final int MODE_HOMESCREEN_WALLPAPER = 0;
    private static final int MODE_LOCKSCREEN_WALLPAPER = 1;
    private static final int MSG_BOOT_COMPLETED = 0x140;
    private static final int MSG_LOCKSCREENWALLPAPER_CHANGED = 0x136;
    private static final String TAG = "LockscreenWallpaperUpdater";
    private static final String WALLPAPERIMAGE_PATH = "/data/data/com.android.tsm_parts/lockscreen_wallpaper.jpg";

    private boolean mBootCompleted = false;
    private BroadcastReceiver mBroadcastReceiver;
    private Handler mHandler;
    private ImageView mLockScreenWallpaperImage;
    private Drawable mLockscreenWallpaperDrawable;
    private RelativeLayout mMainLayout;
    private int mWallpaperModeValue;

    private final String LOCKSCREEN_WALLPAPER_INFO = "com.android.lockscreenwallpaper.CHANGED";
    private static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";

    public LockscreenWallpaperUpdater(Context context){
        super(context);  
	
	final LayoutInflater inflater = LayoutInflater.from(context);
	inflater.inflate(R.layout.keyguard_screen_lockscreenwallpaper,this,true);
	
	mMainLayout = (RelativeLayout) findViewById(R.id.lockscreenwallpaper_root);
	mLockScreenWallpaperImage = (ImageView)findViewById(R.id.lockscreenwallpaper_image);
	
	mHandler = new Handler(){
	    public void handleMessage(Message msg){

	      
	      switch(msg.what) {
		  case MSG_LOCKSCREENWALLPAPER_CHANGED:
		     handleLockScreenWallpaperUpdate(msg.arg1);
		  case MSG_BOOT_COMPLETED:
		      handleBootCompleted();
		  break;
	      }
	    }
	};
	

	IntentFilter filter = new IntentFilter();
	filter.addAction(LOCKSCREEN_WALLPAPER_INFO);
	filter.addAction("android.intent.action.BOOT_COMPLETED");
	filter.addAction("android.intent.action.MEDIA_MOUNTED");
	
	mBroadcastReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent){
		String action = intent.getAction();
		if(LOCKSCREEN_WALLPAPER_INFO.equals(action)){
		    int mode = intent.getIntExtra("isChanged",0);		    
		    mHandler.sendMessage(mHandler.obtainMessage(0x136,mode,0x64));
		} else if("android.intent.action.MEDIA_MOUNTED".equals(action)){
		    Log.d("LockscreenWallpaperUpdater", "MEDIA_MOUNTED");
		    int mode = intent.getIntExtra("isChanged",0);		    
		    mHandler.sendMessage(mHandler.obtainMessage(0x140));
		} else {}
	    }
	};
	 

	context.registerReceiver(mBroadcastReceiver,filter);

	Log.d("LockscreenWallpaperUpdater","add lockscreenwallpaper");
    }

    private Drawable getLockscreenDrawable(){
	Log.d("LockscreenWallpaperUpdater","get drawable (keyguardupdatemonitor private get) ");

	if(mBootCompleted){
	  return mLockscreenWallpaperDrawable;

	}else {
	  Log.d("LockscreenWallpaperUpdater","drawable is null(keyguardupdatemonitor private get) ");
	  setLockscreenDrawable();
	  return mLockscreenWallpaperDrawable;
	}
    }

    private Drawable getWallpaperDrawable(){
	BitmapDrawable wallpaperDrawable;
	Drawable tmpDrawable = null;
	Bitmap bitmap;
	Context mContext = getContext();
		
	File file = new File(WALLPAPERIMAGE_PATH);


	if(file.exists()){
	  Log.d("LockscreenWallpaperUpdater", "wallpaper file exist ");
	  wallpaperDrawable = new BitmapDrawable(mContext.getResources(),WALLPAPERIMAGE_PATH);
	  return wallpaperDrawable;	
	} else {
	  Log.d("LockscreenWallpaperUpdater", "wallpaper file not exist ");
	  return mContext.getResources().getDrawable(R.drawable.default_lockscreen_wallpaper);
	}
      
    }

    private void handleBootCompleted(){
	setLockscreenDrawable();
	mBootCompleted = true;
    }

    private void handleLockScreenWallpaperUpdate(int mode){
	Log.d("LockscreenWallpaperUpdater", "handleLockScreenWallpaperUpdate");
	setLockscreenDrawable();
    }

    private void init(){
	mLockScreenWallpaperImage.setImageDrawable(getLockscreenWallpaper());
    }

    public void cleanUp(){
	removeAllViews();
	getContext().unregisterReceiver(mBroadcastReceiver);
	Log.d("LockscreenWallpaperUpdater","cleanUp()");
    }

    public Drawable getLockscreenWallpaper(){
	Log.d("LockscreenWallpaperUpdater","get drawable (public) ");
	return getLockscreenDrawable();
    }

    public void onPause(){
	Log.d("LockscreenWallpaperUpdater","onPause()");
    }

    public void onResume(){
	Log.d("LockscreenWallpaperUpdater","onResume()");
	init();
    }

    public void onStop(){
	Log.d("LockscreenWallpaperUpdater","onStop()");
    }

    protected void setLockscreenDrawable(){
	Log.d("LockscreenWallpaperUpdater","set drawable");
	mLockscreenWallpaperDrawable = getWallpaperDrawable();
    }
}
	  
	    