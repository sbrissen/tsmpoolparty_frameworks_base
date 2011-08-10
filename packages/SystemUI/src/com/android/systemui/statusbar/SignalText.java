/*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;
import java.util.TimeZone;

public class SignalText extends TextView {

    int dBm = 0;

    int ASU = 0;

    private SignalStrength signal;

    private boolean mAttached;

    private static final int STYLE_HIDE = 0;

    private static final int STYLE_SHOW = 1;

    private static boolean style;

    private int dbmColor;

    Handler mHandler;

    public SignalText(Context context) {
        this(context, null);

    }

    public SignalText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SIGNAL_DBM_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SIGNAL_DBM_CHANGED)) {
                dBm = intent.getIntExtra("dbm", 0);
            }
            updateSettings();
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUSBAR_SHOW_DBM), false,
                    this);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.DBM_COLOR), false,
                    this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private void updateSettings() {
        updateSignalText();

    }

    final void updateSignalText() {
        style = (Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.STATUSBAR_SHOW_DBM, STYLE_HIDE)==1);
        dbmColor = (Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.DBM_COLOR, -1));

        if (style) {
            this.setVisibility(View.VISIBLE);

            String result = Integer.toString(dBm);
	    
	    setTextColor(dbmColor);
            setText(result + " ");
        }else {
            this.setVisibility(View.GONE);
        }
    }
}