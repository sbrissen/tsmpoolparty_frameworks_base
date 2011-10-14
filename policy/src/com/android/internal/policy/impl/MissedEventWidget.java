package com.android.internal.policy.impl;

/*****************************************************************************************************
/
/  Lockscreen Missed Event Widget:
/    -Displays a Notifcation for Email, Mms, and Phone events
/
/  This was modeled from the SGS2 Missed Event widget
/
/  Written By: Scott Brissenden
*******************************************************************************************************/

import java.lang.Enum;
import android.content.BroadcastReceiver;
import com.android.internal.policy.impl.KeyguardScreenCallback;
import android.os.Handler;
import android.app.NotificationManager;
import android.app.PendingIntent;
import com.android.internal.widget.SlidingTab;
import com.android.internal.policy.impl.MissedEventRing;
import android.widget.RelativeLayout;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.content.IntentFilter;
import android.provider.CallLog;
import android.view.View;
import android.view.View.*;
import android.os.Message;
import android.view.LayoutInflater;
import android.content.Intent;
import android.util.Log;
import android.view.animation.*;
import android.database.Cursor;
import android.content.ContentResolver;
import android.text.Selection;
import android.content.ContentProvider;
import android.net.Uri;
import java.net.URISyntaxException;
import android.content.ActivityNotFoundException;
import android.app.PendingIntent.CanceledException;
import android.provider.Settings;

import com.android.internal.R;

public class MissedEventWidget extends RelativeLayout implements SlidingTab.OnTriggerListener, MissedEventRing.OnRingTriggerListener {

  private static final String MISSED_EVENT_ARRIVED = "com.android.server.NotificationManagerService.NotificationArrived";
  private static final String MISSED_EVENT_REMOVED = "com.android.server.NotificationManagerService.NotificationRemoved";
  private static final String MISSED_PHONE_EVENT_ARRIVED = "com.android.phone.NotificationMgr.MissedPhoneNotification";

  private static final String TAG = "MissedEventWidget";

  private static final String MMS_INTENT = "#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;component=com.android.mms/.ui.ConversationList;end";
  private static final String EMAIL_INTENT = "#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;component=com.android.email/.activity.MessageList;end";
  private static final String PHONE_INTENT = "#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;component=com.android.contacts/.DialtactsActivity;end";

  private final String CALL_PKG;
  private final boolean DEBUG;
  private final String EMAIL_PKG;
  private final int MISSED_EVENT_UPDATE = 0x12c2;
  private String MSG_PKG;
  private BroadcastReceiver mBroadcastReceiver;
  private KeyguardScreenCallback mCallback;
  private Handler mHandler;
  private SlidingTab mSelector;
  private MissedEventRing mRingSelector;
  private Context mContext;
  boolean nCallCount = false;
  boolean nMsgCount = false;
  boolean nEmailCount = false;
  int mSelectorStyle;
  boolean mUseTabs;

  public MissedEventWidget(Context context, KeyguardScreenCallback callback){
    super(context);

    DEBUG = true;
    CALL_PKG = "com.android.phone";
    MSG_PKG = "com.android.mms";
    EMAIL_PKG = "cam.android.email";
    mContext = context;

    mSelectorStyle = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_MISSED_EVENT_TYPE, 1));

    mUseTabs = (mSelectorStyle == 1);

    Log.i("MissedEventWidget", new StringBuilder().append(" UseTabs: ").append(mSelectorStyle).toString());
    
    mHandler = new Handler(){    
	public void handleMessage(Message msg){
	  switch(msg.what) {
	    case MISSED_EVENT_UPDATE:
	      handleMissedEventUpdate();
	  }
	}
    };

    mCallback = callback;
    LayoutInflater inflater = LayoutInflater.from(context);
    inflater.inflate(R.layout.keyguard_screen_missed_event,this,true);
  
    IntentFilter filter = new IntentFilter();
    filter.addAction(MISSED_EVENT_ARRIVED);
    filter.addAction(MISSED_EVENT_REMOVED);

    mBroadcastReceiver = new BroadcastReceiver() {    
      public void onReceive(Context context, Intent intent){
	String action = intent.getAction();
	if(action.equals(MISSED_EVENT_ARRIVED) || action.equals(MISSED_EVENT_REMOVED) || action.equals(MISSED_PHONE_EVENT_ARRIVED)){
	  Message msg = mHandler.obtainMessage(MISSED_EVENT_UPDATE);
	  mHandler.sendMessage(msg);
	}
      }
    };

    context.registerReceiver(mBroadcastReceiver,filter);
    init();
  }

  private void init(){
    Log.d("MissedEventWidget","Controller Initiation Running!!");   

    if(mUseTabs){    
      mSelector = (SlidingTab) findViewById(R.id.tabselector);
      mSelector.setHoldAfterTrigger(true,true);
      mSelector.setLeftTabResources(R.drawable.unlock_missed_call_icon,R.drawable.unlock_cue_left,R.drawable.jog_tab_bar_left_missed_call,R.drawable.jog_tab_left_unlock_missed_call,"0");
      mSelector.setRightTabResources(R.drawable.unlock_unread_message_icon,R.drawable.unlock_cue_right,R.drawable.jog_tab_bar_right_unread_msg,R.drawable.jog_tab_right_unlock_unread_msg,"0");
      mSelector.setOnTriggerListener(this);
      mSelector.setVisibility(View.VISIBLE);
      mSelector.setLeftVisibility(View.INVISIBLE);
      mSelector.setRightVisibility(View.INVISIBLE);
    }else{      
      mRingSelector = (MissedEventRing) findViewById(R.id.ringselector);
      mRingSelector.setLeftRingResources(R.drawable.unlock_missed_call_icon,R.drawable.jog_tab_target_red,R.drawable.jog_ring_ring_pressed_red, null);
      mRingSelector.setRightRingResources(R.drawable.unlock_unread_message_icon,R.drawable.jog_tab_target_yellow,R.drawable.jog_ring_ring_pressed_yellow, null);
      mRingSelector.setOnRingTriggerListener(this);
      mRingSelector.setVisibility(View.VISIBLE);
      mRingSelector.setLeftVisibility(View.INVISIBLE);
      mRingSelector.setRightVisibility(View.INVISIBLE);
      mRingSelector.setMiddleVisibility(View.GONE);
    }   
    
    updateMissedEvent();
  }

  private void updateMissedEvent(){
    nCallCount = (Settings.System.getInt(getContext().getContentResolver(),Settings.System.MISSED_PHONE_EVENT, 0) == 1);
    nMsgCount = (Settings.System.getInt(getContext().getContentResolver(),Settings.System.MISSED_MMS_EVENT, 0) == 1);
    nEmailCount = (Settings.System.getInt(getContext().getContentResolver(),Settings.System.MISSED_EMAIL_EVENT, 0) == 1);

    Log.i("MissedEventWidget",new StringBuilder().append("Missed Calls: ").append(nCallCount).toString());
    Log.i("MissedEventWidget",new StringBuilder().append("Missed Mms: ").append(nMsgCount).toString());  
    Log.i("MissedEventWidget",new StringBuilder().append("Missed Email: ").append(nEmailCount).toString());   

    if(mUseTabs){
      mSelector.setLeftIconText("Phone");
      mSelector.setLeftHintText("Missed Call(s)"); 
    }else{
      mRingSelector.setLeftIconText("Phone");
    }

    if(nMsgCount && mUseTabs){
      mSelector.setRightIconText("Sms");
      mSelector.setRightHintText("New Message(s)");
    }else if(nEmailCount && mUseTabs){
      mSelector.setRightIconText("Email");
      mSelector.setRightHintText("New Email(s)");
    }else if(nMsgCount && !mUseTabs){
      mRingSelector.setRightIconText("Sms");
    }else if(nEmailCount && !mUseTabs){
      mRingSelector.setRightIconText("Email");
    }

    if(nCallCount && (nMsgCount || nEmailCount)){
      if(mUseTabs){
	mSelector.setLeftVisibility(View.VISIBLE);
	mSelector.setRightVisibility(View.VISIBLE);
      }else{
	mRingSelector.setLeftVisibility(View.VISIBLE);
	mRingSelector.setRightVisibility(View.VISIBLE);
      }
    }else if(nCallCount && (!nMsgCount && !nEmailCount)){
      if(mUseTabs){
	mSelector.setLeftVisibility(View.VISIBLE);
	mSelector.setRightVisibility(View.INVISIBLE);
      }else{
	mRingSelector.setLeftVisibility(View.VISIBLE);
	mRingSelector.setRightVisibility(View.INVISIBLE);
      }
    }else if(!nCallCount && (nMsgCount || nEmailCount)){
      if(mUseTabs){
	mSelector.setRightVisibility(View.VISIBLE);
	mSelector.setLeftVisibility(View.INVISIBLE);
      }else{
	mRingSelector.setRightVisibility(View.VISIBLE);
	mRingSelector.setLeftVisibility(View.INVISIBLE);
      }
    }else if(!nCallCount && (!nMsgCount || !nEmailCount)){
      if(mUseTabs){
	mSelector.setLeftVisibility(View.INVISIBLE);
	mSelector.setLeftVisibility(View.INVISIBLE);
      }else{
	mRingSelector.setLeftVisibility(View.INVISIBLE);
	mRingSelector.setLeftVisibility(View.INVISIBLE);
      }
    }
  }

  public void cleanUp(){
    mCallback = null;
    getContext().unregisterReceiver(mBroadcastReceiver);
    Log.d("MissedEventWidget","cleanUp()");
  }

  protected void handleMissedEventUpdate(){
    updateMissedEvent();
    invalidate();
  }

  public void onGrabbedStateChange(View v, int grabbedState){
    if(grabbedState != 0){
      mCallback.pokeWakelock();
    }
  }

  public void onPause(){
    Log.d("MissedEventWidget","onPause()");
  }

  public void onResume(){
    Log.d("MissedEventWidget","onResume()");
    handleMissedEventUpdate();
  }

  public void onTrigger(View v, int whichHandle){
    Intent i;
    if(!mCallback.isSecure()){   
      if(whichHandle != 1){ 
	if(whichHandle != 2){  
	
	}else{
	 try {
	  if(nMsgCount){
             i = Intent.parseUri(MMS_INTENT, 0);
             i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivity(i);
	  }else if(nEmailCount){
	     i = Intent.parseUri(EMAIL_INTENT, 0);
             i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivity(i);
	  } 
             mCallback.goToUnlockScreen();
	  } catch (URISyntaxException e) {
          } catch (ActivityNotFoundException e) {
          }
	}
      }else{
	 try {
	     i = Intent.parseUri(PHONE_INTENT, 0);
             i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivity(i);
	    mCallback.goToUnlockScreen();
	  } catch (URISyntaxException e) {
          } catch (ActivityNotFoundException e) {
          }
      }
    }else{
      if(mUseTabs){
	mSelector.cancelAnimation();
      }else{
	mRingSelector.cancelAnimation();
      }
    }
    mCallback.goToUnlockScreen();
  }
    public void onRingTrigger(View v, int whichRing, int whichApp) {
	Intent i;
        if (whichRing == MissedEventRing.OnRingTriggerListener.RIGHT_RING) {
	 try {
	  if(nMsgCount){
             i = Intent.parseUri(MMS_INTENT, 0);
             i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivity(i);
	  }else if(nEmailCount){
	     i = Intent.parseUri(EMAIL_INTENT, 0);
             i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivity(i);
	  } 
             mCallback.goToUnlockScreen();
	  } catch (URISyntaxException e) {
          } catch (ActivityNotFoundException e) {
          }
      } else if (whichRing == MissedEventRing.OnRingTriggerListener.LEFT_RING) {
         	 try {
	     i = Intent.parseUri(PHONE_INTENT, 0);
             i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivity(i);
	    mCallback.goToUnlockScreen();
	  } catch (URISyntaxException e) {
          } catch (ActivityNotFoundException e) {
          }
        }
    }


}