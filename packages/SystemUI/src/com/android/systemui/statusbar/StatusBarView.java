/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.graphics.*;
import android.view.animation.*;
import java.lang.Math;
import android.database.ContentObserver;
import android.provider.Settings;
import android.content.ContentResolver;
import android.os.Handler;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import com.android.systemui.R;

public class StatusBarView extends FrameLayout {
    private static final String TAG = "StatusBarView";

    static final int DIM_ANIM_TIME = 400;
    
    StatusBarService mService;
    boolean mTracking;
    int mStartX, mStartY;
    ViewGroup mNotificationIcons;
    ViewGroup mStatusIcons;
    View mDate;
    FixedSizeDrawable mBackground;
 
    View mBatteryIndicator;
    View mBatteryChargingIndicator;
    int mBatteryColor;
    boolean mBatteryAutoColor;
    boolean mScreenOn = true;
    private boolean mAttached;

    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_BATTERY_STYLE), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_BATTERY_STYLE2), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.BATTERY_AUTO_COLOR), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.BATTERY_COLOR_AUTO_CHARGING), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.BATTERY_COLOR_AUTO_REGULAR), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.BATTERY_COLOR_AUTO_MEDIUM), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.BATTERY_COLOR_AUTO_LOW), false, this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.BATTERY_COLOR), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
    
    public StatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

       // updateSettings();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mNotificationIcons = (ViewGroup)findViewById(R.id.notificationIcons);
        mStatusIcons = (ViewGroup)findViewById(R.id.statusIcons);
        mDate = findViewById(R.id.date);

        mBackground = new FixedSizeDrawable(mDate.getBackground());
        mBackground.setFixedBounds(0, 0, 0, 0);
        mDate.setBackgroundDrawable(mBackground);

	mBatteryIndicator = findViewById(R.id.battery_indicator);
	mBatteryChargingIndicator = findViewById(R.id.battery_indicator_charging);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mService.onBarViewAttached();

     if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_SCREEN_OFF);
	    filter.addAction(Intent.ACTION_SCREEN_ON);

            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
	    	    
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
		mScreenOn = false;
            }
	    if (action.equals(Intent.ACTION_SCREEN_ON)) {
		mScreenOn = true;
            }
        }
    };
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mService.updateExpandedViewPos(StatusBarService.EXPANDED_LEAVE_ALONE);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

	ContentResolver resolver = mContext.getContentResolver();

        // put the date date view quantized to the icons
        int oldDateRight = mDate.getRight();
        int newDateRight;

	int mShowBatteryIndicator;
	int mShowBatteryIndicator2;	
	
        newDateRight = getDateSize(mNotificationIcons, oldDateRight,
                getViewOffset(mNotificationIcons));
        if (newDateRight < 0) {
            int offset = getViewOffset(mStatusIcons);
            if (oldDateRight < offset) {
                newDateRight = oldDateRight;
            } else {
                newDateRight = getDateSize(mStatusIcons, oldDateRight, offset);
                if (newDateRight < 0) {
                    newDateRight = r;
                }
            }
        }
        int max = r - getPaddingRight();
        if (newDateRight > max) {
            newDateRight = max;
        }

        mDate.layout(mDate.getLeft(), mDate.getTop(), newDateRight, mDate.getBottom());
        mBackground.setFixedBounds(-mDate.getLeft(), -mDate.getTop(), (r-l), (b-t));

        mShowBatteryIndicator = (Settings.System
                .getInt(resolver, Settings.System.STATUSBAR_BATTERY_STYLE, 0));
        mShowBatteryIndicator2 = (Settings.System
                .getInt(resolver, Settings.System.STATUSBAR_BATTERY_STYLE2, 0));
	
	if(mShowBatteryIndicator == 1 || mShowBatteryIndicator2 == 1){
	    mBatteryIndicator.setVisibility(VISIBLE);

        mBatteryAutoColor = (Settings.System
                .getInt(resolver, Settings.System.BATTERY_AUTO_COLOR, 0) == 1);
	
	    Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null,
	    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    int level = batteryIntent.getIntExtra("level", 0);
	    boolean plugged = batteryIntent.getIntExtra("plugged", 0) != 0;

	    if(!mBatteryAutoColor){
		mBatteryColor = Settings.System.getInt(resolver, Settings.System.BATTERY_COLOR, -1);
		mBatteryIndicator.setBackgroundColor(mBatteryColor);
	    }else if (plugged){
		  mBatteryColor = Settings.System.getInt(resolver, Settings.System.BATTERY_COLOR_AUTO_CHARGING, -1);
	      }else if (level >= 60){
		  mBatteryColor = Settings.System.getInt(resolver, Settings.System.BATTERY_COLOR_AUTO_REGULAR, -1);
	      }else if (level <= 15){
		  mBatteryColor = Settings.System.getInt(resolver, Settings.System.BATTERY_COLOR_AUTO_LOW, -1);
	      }else {
		  mBatteryColor = Settings.System.getInt(resolver, Settings.System.BATTERY_COLOR_AUTO_MEDIUM, -1);
	      }

	    mBatteryIndicator.setBackgroundColor(mBatteryColor);
	    mBatteryIndicator.layout(mBatteryIndicator.getLeft(),mBatteryIndicator.getTop(), ((r-l) * level) / 100,2);

	    if(plugged){
		mBatteryChargingIndicator.setVisibility(VISIBLE);
		mBatteryChargingIndicator.setBackgroundColor(mBatteryColor);
		int chargingWidth = Math.min(5,((r-l) * (100 - level)) / 2);
		mBatteryChargingIndicator.layout(r,t,(r+chargingWidth),t+2);
	    
		Animation a = new TranslateAnimation(0,(float)level-(r-l),0,0);
		a.setInterpolator(new AccelerateInterpolator());
		a.setDuration(2000);
		a.setRepeatCount(-1);
		a.setRepeatMode(1);
		if(mScreenOn){
		  mBatteryChargingIndicator.startAnimation(a);
		}
		else {
		  mBatteryChargingIndicator.clearAnimation();
		}
	    }else {
		mBatteryChargingIndicator.setVisibility(GONE);
	    }
	} else {
	    mBatteryChargingIndicator.clearAnimation();
	    mBatteryIndicator.setVisibility(GONE);
	    mBatteryChargingIndicator.setVisibility(GONE);
	}
    }

    /**
     * Gets the left position of v in this view.  Throws if v is not
     * a child of this.
     */
    private int getViewOffset(View v) {
        int offset = 0;
        while (v != this) {
            offset += v.getLeft();
            ViewParent p = v.getParent();
            if (v instanceof View) {
                v = (View)p;
            } else {
                throw new RuntimeException(v + " is not a child of " + this);
            }
        }
        return offset;
    }

    private int getDateSize(ViewGroup g, int w, int offset) {
        final int N = g.getChildCount();
        for (int i=0; i<N; i++) {
            View v = g.getChildAt(i);
            int l = v.getLeft() + offset;
            int r = v.getRight() + offset;
            if (w >= l && w <= r) {
                return r;
            }
        }
        return -1;
    }

    /**
     * Ensure that, if there is no target under us to receive the touch,
     * that we process it ourself.  This makes sure that onInterceptTouchEvent()
     * is always called for the entire gesture.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            mService.interceptTouchEvent(event);
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mService.interceptTouchEvent(event)
                ? true : super.onInterceptTouchEvent(event);
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();

        int mShowBatteryIndicator = (Settings.System
                .getInt(resolver, Settings.System.STATUSBAR_BATTERY_STYLE, 0));
        int mShowBatteryIndicator2 = (Settings.System
                .getInt(resolver, Settings.System.STATUSBAR_BATTERY_STYLE2, 0));

	if(mShowBatteryIndicator == 1 || mShowBatteryIndicator2 == 1){
	    Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null,
	    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    
	    boolean plugged = batteryIntent.getIntExtra("plugged", 0) != 0;

	     mBatteryIndicator.setVisibility(VISIBLE);
	     if(plugged){
		  mBatteryChargingIndicator.setVisibility(VISIBLE);
	      }
	} else {
	    mBatteryIndicator.setVisibility(GONE);
	    mBatteryChargingIndicator.setVisibility(GONE);
	}
    }
}

