 
/*
* Copyright (C) 2006 The Android Open Source Project
* Patched by Sven Dawitz; Copyright (C) 2011 CyanogenMod Project
*
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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.database.ContentObserver;

public class LeftClock extends Clock {

    public LeftClock(Context context) {
        this(context, null);
    }

    public LeftClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LeftClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();

        updateSettings();
    }

    @Override
    protected void updateSettings() {
        super.updateSettings();

        ContentResolver resolver = mContext.getContentResolver();

        int mClockPosition = (Settings.System.getInt(resolver, Settings.System.STATUSBAR_CLOCK_POSITION, 0));
	int mClockStyle = Settings.System.getInt(resolver, Settings.System.STATUSBAR_CLOCK_STYLE, 2);
        if (mClockPosition == 2 && mClockStyle != 3)
            setVisibility(View.VISIBLE);
        else
            setVisibility(View.GONE);

    }
}