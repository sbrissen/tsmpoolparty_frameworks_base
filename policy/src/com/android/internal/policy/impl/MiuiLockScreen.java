package com.android.internal.policy.impl;

/*****************************************************************************************************
/	VERY ALPHA
/  MIUI Lockscreen:
/    
/  Written By: Scott Brissenden
/
*******************************************************************************************************/

import java.lang.Runnable;
import android.widget.*;
import android.widget.ImageView.ScaleType;
import android.graphics.drawable.*;
import android.view.View;
import android.view.MotionEvent;
import com.android.internal.policy.impl.KeyguardScreenCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.SlidingPanel;
import com.android.internal.policy.impl.LockscreenWallpaperUpdater;
import com.android.internal.policy.impl.LockscreenInfo;
import com.android.internal.policy.impl.MusicWidget;
import com.android.internal.policy.impl.MissedEventWidget;
import android.view.animation.Animation;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.os.PowerManager;
import com.android.internal.policy.impl.KeyguardUpdateMonitor;
import android.content.Context;
import android.content.ContentResolver;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.internal.policy.impl.DigitalClock;
import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ColorStateList;
import android.text.format.DateFormat;
import android.util.Log;
import android.media.AudioSystem;
import android.provider.Telephony.*;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.content.IntentFilter;
import java.io.File;
import com.android.internal.telephony.IccCard;
import android.view.animation.AnimationUtils;
import android.os.Vibrator;
import java.net.URISyntaxException;
import android.content.ActivityNotFoundException;
import android.view.Gravity;
import android.media.AudioManager;

import com.android.internal.R;

class MiuiLockScreen extends FrameLayout implements KeyguardScreen, KeyguardUpdateMonitor.InfoCallback, KeyguardUpdateMonitor.SimStateCallback,
    SlidingPanel.OnPanelTriggerListener, View.OnTouchListener{

    private static final int BACKGROUND_MUSIC_SHOW_HAS_ALBUM = 2;
    private static final int BACKGROUND_MUSIC_SHOW_NONE = 1;
    private static final int BACKGROUND_MUSIC_SHOW_NO_ALBUM = 3;
    private static final int BACKGROUND_NONE = 0;
    private static final int CHECK_STREAM_MUSIC_DELAY = 1000;
    private static final int CONTROL_VIEW_CALL = 1;
    private static final int CONTROL_VIEW_DATE = 3;
    private static final int CONTROL_VIEW_MUSIC = 4;
    private static final int CONTROL_VIEW_NONE = 0;
    private static final int CONTROL_VIEW_SMS = 2;
    private static String[] CallLog_COLUMNS = null;
    private static final boolean DBG = false;
    private static final String ENABLE_MENU_KEY_FILE = "/data/local/enable_menu_key";
    private static final long MAKR_PREVIEW_READ_DELAY = 0x7d0L;
    private static final int MAX_VISIBLE_ITEM_NUM = 2;
    private static final int MUSIC_SHOW_NONE = 0;
    private static final int MUSIC_SHOW_PLAY = 2;
    private static final int MUSIC_SHOW_STOP = 1;
    private static final int QUERY_TOKEN = 0x35;
    private static final int SMS_RECEIVED_WAKE_UP_DELAY = 0x61a8;
    private static final String TAG = "MiuiLockScreen";
    private static int mBatteryInfoState;
    private static int mPlugedState;
    private static Runnable sLongPressVibration;

    private boolean isPaused = false;
    private TextView mAlarm;
    private Drawable mAlarmIcon;
    //private PreviewCursorAdpater mAdapter;
    private ImageView mAlbumMask;
    private ImageView mAlbumView;
    private FrameLayout mAlbumWraper;
    private View mBackgroundMask;
    private int mBatteryLevel = 0x64;  
    private final KeyguardScreenCallback mCallback;
    //private Context mContext;
    //private CallLogPreviewListAdapter mCallsAdapter;
    private View mCallsControlView;
    //private QueryHandler mCallsHandler;
   // private final CallsObserver mCallsObserver;
    private ListView mCallsPreviewList;  
    private TextView mCarrier;
    private TextView mCustomMsg;
    private Runnable mCheckStreamMusicRunnable;
    private FrameLayout mControlView;
    private int mControlViewType;
    private int mCreationOrientation;
    private ListView mCurrentPreviewList;
    private LockscreenWallpaperUpdater mLockscreenWallpaperUpdater;
    private RelativeLayout mMainLayout;
    private View mEmergencyCall;
    private TextView mEmergencyCallText;
    private boolean mEnableMenuKeyInLockScreen;
    private final Animation mFadeoutAnim;
    private final Handler mHandler;
    private View mHintView;
    private boolean mIsOnlineSongBlocking;
    private int mKeyboardHidden;
    private long mLastGrabTime;
    private LockPatternUtils mLockPatternUtils;
    //private LockScreenAlbumController mLockScreenAlbumController;
    private String mMessagingTabApp;
    private View mMusicControl;
    private ImageView mMusicPlayPauseButton;
    private int mMusicStatus;
    private TextView mMusicTitle;
    private String mNextAlarm;
    //private final Handler mObserverHandler;
    private BroadcastReceiver mPlayerStatusListener;
    private boolean mPluggedIn = false;
    private PowerManager mPowerManager;
    private TextView mScreenLocked;
    private SlidingPanel mSelector;
    private boolean mShowingBatteryInfo = false;
    //private SmsPreviewListAdapter mSmsAdapter;
    private View mSmsControlView;
    //private QueryHandler mSmsHandler;
    //private final SmsObserver mSmsObserver;
    private ListView mSmsPreviewList;
    private Status mStatus;
    private ImageView mTempAlbumView;
    private View mTimeView;
    private LinearLayout mTopLayout;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private LinearLayout mBoxLayout;
    private LockscreenInfo mLockscreenInfo;
    private LinearLayout mMusicLayoutBottom;
    private LinearLayout mMusicLayoutTop;
    private MusicWidget mMusicWidget; 
    private MissedEventWidget mMissedEvent;
    private LinearLayout mMissedLayout;
    private ImageButton mPlayIcon;
    private ImageButton mPauseIcon;
    private ImageButton mRewindIcon;
    private ImageButton mForwardIcon;
    private AudioManager am = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
    private boolean mWasMusicActive = am.isMusicActive();
    private boolean mIsMusicActive = false;
    private AudioManager mAudioManager;

    private String mCustomAppActivity = (Settings.System.getString(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_CUSTOM_APP_ACTIVITY));

    private boolean mShowingInfo = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_SHOW_INFO, 0) == 1);

    private boolean mLockMusicControls = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_MUSIC_CONTROLS, 0) == 1);

    private boolean mLockAlwaysMusic = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_ALWAYS_MUSIC_CONTROLS, 0) == 1);

    private int mSgsMusicLoc = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_SGSMUSIC_CONTROLS_LOC, 1));

    private boolean mSgsMusicControls = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_SGSMUSIC_CONTROLS, 1) == 1);

    private boolean mAlwaysSgsMusicControls = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_ALWAYS_SGSMUSIC_CONTROLS, 0) == 1);

    private boolean mShowMissedEvent = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.LOCKSCREEN_MISSED_EVENT, 0) == 1);

    enum Status {
        /**
         * Normal case (sim card present, it's not locked)
         */
        Normal(true),

        /**
         * The sim card is 'network locked'.
         */
        NetworkLocked(true),

        /**
         * The sim card is missing.
         */
        SimMissing(false),

        /**
         * The sim card is missing, and this is the device isn't provisioned, so we don't let
         * them get past the screen.
         */
        SimMissingLocked(false),

        /**
         * The sim card is PUK locked, meaning they've entered the wrong sim unlock code too many
         * times.
         */
        SimPukLocked(false),

        /**
         * The sim card is locked.
         */
        SimLocked(true);

        private final boolean mShowStatusLines;

        Status(boolean mShowStatusLines) {
            this.mShowStatusLines = mShowStatusLines;
        }

        /**
         * @return Whether the status lines (battery level and / or next alarm) are shown while
         *         in this state.  Mostly dictated by whether this is room for them.
         */
        public boolean showStatusLines() {
            return mShowStatusLines;
        }
    }
/*    abstract class PreviewCursorAdapter extends ResourceCursorAdapter{
	private static HashMap<String, SoftReference> mContacts = new HashMap<String, SoftReference>();
	private boolean mLoading;
	private boolean mUseDefaultCount;
	public PreviewCursorAdapter(Context context, int layout, Cursor c){
	    super(context,layout,c);
	    mLoading = true;
	    mUseDefaultCount = true;
	}
	public void bindView(View view, Context context, Cursor cursor){
	}
	public void enableDefaultCount(boolean enable){
	    mUseDefaultCount = enable;
	}
	protected String formatDate(long date){
	    String formatHour;
	    if(DateFormat.is24HourFormat(mContext)){
		formatHour = "MMM d, kk:mm";
	    }else{
		formatHour = "MMM d,aa h:mm";
	    }
	    String dateString = format(formatHour,date);
	    String day = format("MMM d",Calendar.getInstance().getTimeInMillis()).toString();
	    if(dateString.startsWith(day)){
		return (dateString.split(","))[0].trim();
	    }else{
		return new StringBuilder().append(dateString.split(",")[0]).append(mContext.getString(R.string.date_day)).toString();
	    }
	}
	protected ContactInfo getContact(String phoneNumber){
	    ContactInfo info = null;
	    if(mContacts.containsKey(phoneNumber)){
		SoftReference infoReference = (SoftReference) mContacts.get(phoneNumer);
		info = (ContactInfo) infoReference.get();
	    }
	    Cursor cursor = null;
	    try{
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,Uri.encode(phoneNumber));
		cursor = mContext.getContentResolver().query(uri,new String[0]("display_name"),"0","0","0");
		if(cursor != null || cursor.moveToFirst()){  else cond_7c
		    info = new ContactInfo(this);
		}*/
		    
		    

   /* class CallLogPreviewListAdapter */

    MiuiLockScreen(Context context, Configuration configuration, LockPatternUtils lockPatternUtils, KeyguardUpdateMonitor updateMonitor, KeyguardScreenCallback callback){
	super(context);
	
	mStatus = Status.Normal;
	mLastGrabTime = 0x7fffffffffffffffL;
	//Handler mObserverHandler = new Handler();
	//mContext = context;
	ContentResolver resolver = mContext.getContentResolver();

	sLongPressVibration = new Runnable() {
	    Vibrator sVibrator = new Vibrator();
	    public void run(){
		sVibrator.vibrate(0x1e);
	    }
	};

	CallLog_COLUMNS = new String[5];
	CallLog_COLUMNS[0] = "_id";
	CallLog_COLUMNS[1] = "number";
	CallLog_COLUMNS[2] = "date";
	CallLog_COLUMNS[3] = "duration";
	CallLog_COLUMNS[4] = "name";

	//mCallsObserver = new CallsObserver(mObserverHandler);
	//mSmsObserver = new SmsObserver(mObserverHandler);
	mHandler = new Handler() {
	    public void run(){
	    }
	};
	/*mPlayerStatusListener = new BroadcastReceiver() {    
	    public void onReceive(Context context, Intent intent){
	      String action = intent.getAction();
	      boolean isPlaying = intent.getBooleanExtra("playing",false);
	      if(isPlaying){
		if(action.equals("com.miui.player.metachanged")){    
		  String extra = intent.getStringExtra("other");
		  if("meta_changed_track".equals(extra)){  
		      setTrackInfo(intent);
		      if(mLockScreenAlbumController != null){  
			  mLockScreenAlbumController.requestAlbum(intent);
		      }
		  }else if("meta_changed_album".equals(extra)){
		      if(mLockScreenAlbumController != null){
			  mLockScreenAlbumController.requestAlbum(intent,true);
		      }
		  }else if("lockscreen.action.SONG_METADATA_UPDATED".equals(extra)){
		      setTrackInfo(intent);
		      if(mLockScreenAlbumController != null){
			  mLockScreenAlbumController.setAlbumInfo(intent);
		      }
		  }else if("com.miui.player.refreshprogress".equals(extra)){
		      mIsOnlineSongBlocking = intent.getBooleanExtra("block",false);
		  }
		}
	      } 
	  }		
	}; */

	mCheckStreamMusicRunnable = new Runnable() {
	    public void run(){
		updateMusic();
		postDelayed(mCheckStreamMusicRunnable,0x3e8L);
	    }
	};

	Resources res = context.getResources();
	mLockPatternUtils = lockPatternUtils;
	mUpdateMonitor = updateMonitor;
	mCallback = (KeyguardScreenCallback)callback;
	mEnableMenuKeyInLockScreen = shouldEnableMenuKey();
	mCreationOrientation = configuration.orientation;
	mKeyboardHidden = configuration.hardKeyboardHidden;
	mAlbumWraper = new FrameLayout(mContext);
	mAlbumWraper.setVisibility(View.GONE);
	addView(mAlbumWraper,new FrameLayout.LayoutParams(-1,-1,0x30));
	mAlbumView = new ImageView(mContext);
	mAlbumView.setScaleType(ScaleType.CENTER_CROP);
	mAlbumWraper.addView(mAlbumView,new FrameLayout.LayoutParams(-1,-1,0x30));
	mMusicWidget = new MusicWidget(mContext,callback,updateMonitor);	
	mTempAlbumView = new ImageView(mContext);
	mTempAlbumView.setScaleType(ScaleType.CENTER_CROP);
	mTempAlbumView.setVisibility(View.GONE);
	mAlbumWraper.addView(mTempAlbumView,new FrameLayout.LayoutParams(-1,-1,0x30));
	mAlbumMask = new ImageView(mContext);
	mAlbumMask.setScaleType(ScaleType.FIT_XY);
	mAlbumMask.setImageResource(R.drawable.lock_screen_music_bg);
	mAlbumWraper.addView(mAlbumMask,new FrameLayout.LayoutParams(-1,-2,0x50));
	mBackgroundMask = new View(mContext);
	mBackgroundMask.setBackgroundResource(R.drawable.lock_screen_battery_bg);
	mBackgroundMask.setVisibility(View.GONE);
	addView(mBackgroundMask,new FrameLayout.LayoutParams(-1,-1,0x50));
	LayoutInflater inflater = LayoutInflater.from(context);
	inflater.inflate(R.layout.keyguard_screen_tab_unlock_miui,this,true);
	mCarrier = (TextView) findViewById(R.id.carrier);
	mCarrier.setSelected(true);

	mMissedLayout = (LinearLayout) findViewById(R.id.missedevent);
	mMissedEvent = new MissedEventWidget(context,callback);

	if(mShowMissedEvent){	    
	    mMissedLayout.addView(mMissedEvent);
	}
	
	mMusicLayoutBottom = (LinearLayout) findViewById(R.id.musicwidget_bottom);
	mMusicLayoutTop = (LinearLayout) findViewById(R.id.musicwidget_top);
	if(mSgsMusicLoc == 1 && mSgsMusicControls){
	  mMusicWidget.setTopLayout();
	  mMusicLayoutTop.addView(mMusicWidget);
	}else if(mSgsMusicLoc == 2 && mSgsMusicControls){
	  mMusicWidget.setBottomLayout();
	  mMusicLayoutBottom.addView(mMusicWidget);
	}
	
	mMainLayout = (RelativeLayout) findViewById(R.id.wallpaper_panel);
	mLockscreenWallpaperUpdater = new LockscreenWallpaperUpdater(context);
	mLockscreenWallpaperUpdater.setVisibility(View.VISIBLE);
	mMainLayout.addView(mLockscreenWallpaperUpdater,0);

	mLockscreenInfo = new LockscreenInfo(context,updateMonitor,configuration,lockPatternUtils);
	mBoxLayout = (LinearLayout) findViewById(R.id.lock_box);

	if(mShowingInfo){
	  mBoxLayout.addView(mLockscreenInfo);
	}

        mPlayIcon = (ImageButton) findViewById(R.id.musicControlPlay);
        mPauseIcon = (ImageButton) findViewById(R.id.musicControlPause);
        mRewindIcon = (ImageButton) findViewById(R.id.musicControlPrevious);
        mForwardIcon = (ImageButton) findViewById(R.id.musicControlNext);

        mScreenLocked = (TextView) findViewById(R.id.screenLocked);	
        mEmergencyCallText = (TextView) findViewById(R.id.emergencyCall);
	mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyCallText);
	mEmergencyCall = (View) mEmergencyCallText.getParent();
        mEmergencyCall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.takeEmergencyCallAction();
            }
        });

	mTimeView = inflater.inflate(R.layout.lock_screen_tab_unlock_time,this,false);
	mHintView = inflater.inflate(R.layout.lock_screen_tab_unlock_hint,this,false);
	mFadeoutAnim = AnimationUtils.loadAnimation(mContext,R.anim.lock_screen_controlview_fade_out);
	setupSlidingPanel();
	setupCallsPreviewList();
	setupSmsPreviewList();
	setupMusicControl();
	setFocusable(true);
	setFocusableInTouchMode(true);
	setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
	setChildrenDrawnWithCacheEnabled(true);
	updateMonitor.registerInfoCallback(this);
	updateMonitor.registerSimStateCallback(this);
	ImageView statusBarBg = new ImageView(mContext);
	statusBarBg.setScaleType(ScaleType.FIT_XY);
	statusBarBg.setBackgroundResource(R.drawable.lock_screen_status_bar_bg);
	addView(statusBarBg,new FrameLayout.LayoutParams(-1,-2,0x30));
	mPowerManager = (PowerManager) mContext.getSystemService("power");
	mCustomMsg = (TextView) findViewById(R.id.customMsg);
	String r = (Settings.System.getString(resolver, Settings.System.LOCKSCREEN_CUSTOM_MSG));
	mCustomMsg.setSelected(true);
	mCustomMsg.setText(r);

       mPlayIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.pokeWakelock();
                refreshMusicStatus();
                if (!am.isMusicActive()) {
                    mPauseIcon.setVisibility(View.VISIBLE);
                    mPlayIcon.setVisibility(View.GONE);
                    mRewindIcon.setVisibility(View.VISIBLE);
                    mForwardIcon.setVisibility(View.VISIBLE);
                    sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                }
            }
        });

        mPauseIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.pokeWakelock();
                refreshMusicStatus();
                if (am.isMusicActive()) {
                    mPlayIcon.setVisibility(View.VISIBLE);
                    mPauseIcon.setVisibility(View.GONE);
                    mRewindIcon.setVisibility(View.GONE);
                    mForwardIcon.setVisibility(View.GONE);
                    sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                }
            }
        });

        mRewindIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.pokeWakelock();
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        });

        mForwardIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCallback.pokeWakelock();
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
        });

    }

    /*private void callAndSmsHandle(ListView previewList, PreviewCursorAdpater adapter, View controlView){
	if(!mCallback.isSecure() && adapter > 0){
	    mAdpater = adapter;
	    mAdapter.enableDefaultCount(false);
	    mCurrentPreviewList = previewList;
	    markReadDelayVibrate();
	    previewList.setAdapter(adapter);
	}else{
	    previewList.setAdapter(null);
	}
	setControlView(controlView, null);
    }*/

    private void enableMusicControl(boolean enable){
	if(enable){
	  mMusicControl.setVisibility(View.VISIBLE);
	}else{
	  mMusicControl.setVisibility(View.INVISIBLE);
	}

	if(enable){
	  mControlViewType = 4;
	  setControlView(mMusicControl,null);
	}else{
	  mMusicStatus = 0;
	  if(mControlViewType == 4){
	    updateControlView();
	  }
	}
    }

    static CharSequence getCarrierString(CharSequence telephonyPlmn, CharSequence telephonySpn) {
        if (telephonyPlmn != null && telephonySpn == null) {
            return telephonyPlmn;
        } else if (telephonyPlmn != null && telephonySpn != null) {
            return telephonyPlmn + "|" + telephonySpn;
        } else if (telephonyPlmn == null && telephonySpn != null) {
            return telephonySpn;
        } else {
            return "";
        }
    }

    private Status getCurrentStatus(IccCard.State simState) {
        boolean missingAndNotProvisioned = (!mUpdateMonitor.isDeviceProvisioned()
                && simState == IccCard.State.ABSENT);
        if (missingAndNotProvisioned) {
            return Status.SimMissingLocked;
        }

        switch (simState) {
            case ABSENT:
                return Status.SimMissing;
            case NETWORK_LOCKED:
                return Status.SimMissingLocked;
            case NOT_READY:
                return Status.SimMissing;
            case PIN_REQUIRED:
                return Status.SimLocked;
            case PUK_REQUIRED:
                return Status.SimPukLocked;
            case READY:
                return Status.Normal;
            case UNKNOWN:
                return Status.SimMissing;
        }
        return Status.SimMissing;
    }	   

    private void markReadDelayVibrate(){
	if(mLastGrabTime == 0x7fffffffffffffffL){
	  mLastGrabTime = System.currentTimeMillis();
	  postDelayed(sLongPressVibration,0x7d0L);
	}
    }

  /*  private void noHandle(){
	removeCallbacks(sLongPressVibration);
	if(((System.CurrentTimeMillis() - mLastGrabTime).compareTo(0x7d0)) > 0){
	    if(mAdapter != null){
		int MAX_MARK_READ_COUNT = ((mCurrentPreviewList.getLastVisiblePosition() - mCurrentPreviewList.getFirstVisiblePosition()) - mCurrentPreviewList.getHeaderViewsCount()) + 1;
		int i = MAX_MARK_READ_COUNT - 1;
		if(i >= 0){
		  mAdapter.markRead(mContext, i);
		}else{
		  mAdapter.enableDefaultCount(true);
		  int count = mAdapter.getCount();
		  Intent intent = new Intent("android.intent.action.APPLICATION_MESSAGE_UPDATE");
		  if(count > MAX_MARK_READ_COUNT){
		    intent.putExtra("android.intent.extra.update_application_message",String.valueOf(count - MAX_MARK_READ_COUNT));
		  }else{
		     intent.putExtra("android.intent.extra.update_application_message","0");
		  }
		  if(mAdapter == mCallsAdapter){    else cond_7b
		     intent.putExtra("android.intent.extra.update_application_flatten_name","com.android.contacts/.TwelveKeyDialer");
		     startCallsQuery();
		  }else if(mAdapter == mSmsAdapter){
		      intent.putExtra("android.intent.extra.update_application_flatten_name","com.android.mms/.ui.ConversationList");
		      startSmsQuery();
		  }
		  mContext.sendBroadcast(intent);
		}		  
	    }
	}else{
	    mLastGrabTime = 0x7fffffffffffffffL;
	}
    } */

    private void refreshBatteryStringAndIcon() {
	String info = null;
	int backgroundId = 0;
	if(mShowingBatteryInfo){
	    mPluggedIn = false;
	    if(mPlugedState == 1){
		if(mBatteryLevel >= 0x64){
		    mBatteryLevel = 0x63;
		    info = getContext().getString(R.string.lockscreen_charged);
		    backgroundId = 3;
		}else{
		    info = getContext().getString(R.string.lockscreen_low_battery);
		    backgroundId = 1;
		}
	    }else{
		Object[] battLevel = new Object[1];
		battLevel[0] = Integer.valueOf(mBatteryLevel);
		info = getContext().getString(R.string.lockscreen_plugged_in,battLevel[0]);
		backgroundId = 2;
	    }
	}

	mSelector.setBatteryInfo(info);
	mSelector.setBackgroundFor(backgroundId);
    }

    private void resetStatusInfo(KeyguardUpdateMonitor updateMonitor) {
        mShowingBatteryInfo = updateMonitor.shouldShowBatteryInfo();
        mPluggedIn = updateMonitor.isDevicePluggedIn();
        mBatteryLevel = updateMonitor.getBatteryLevel();

        mStatus = getCurrentStatus(updateMonitor.getSimState());
        updateLayout(mStatus);

        refreshBatteryStringAndIcon();
      
    }

    private void sendMediaButtonBroadcast(int action, int keycode){
	long eventtime = SystemClock.uptimeMillis();
	KeyEvent event = new KeyEvent(eventtime,eventtime,action,keycode,0);
	Intent intent = new Intent("android.intent.action.MEDIA_BUTTON",null);
	intent.putExtra("android.intent.extra.KEY_EVENT",KeyEvent.changeFlags(event,8));
	if(!isPaused && mPowerManager.isScreenOn()){
	    mCallback.pokeWakelock();
	}
    }

    private void setControlView(View view, MarginLayoutParams params){
      try{
	  view.setDrawingCacheEnabled(true);
	if(view != null){ 
	    boolean useDefaultParams = false;
	    if(params == null){ 
		params = (MarginLayoutParams)view.getLayoutParams();
		if(params == null){
		    params = new FrameLayout.LayoutParams(-1,-2,0x50);
		}else{
		    useDefaultParams = true;
		}
	    }else{
	      ViewGroup parent = (ViewGroup) view.getParent();
	      if(!mControlView.equals(parent)){
		mControlView.removeAllViews();
		mControlView.addView(view,params);
		mControlView.setVisibility(View.VISIBLE);
	      }else if(useDefaultParams){
		
	      }
	    }
	}else{
	    mControlView.removeAllViews();
	    mControlView.setVisibility(View.INVISIBLE);
	}
      }catch(IllegalStateException ise){}
    }

    private void setTrackInfo(Intent intent){
      String title = KeyguardViewMediator.TrackId();
      String artist = KeyguardViewMediator.Artist();
      mMusicTitle.setText(new StringBuilder().append(artist).append(" / ").append(artist).toString());
    }
      
    private void setupCallsPreviewList(){
	mCallsControlView = inflate(mContext,R.layout.lockscreen_call_preview_list,null);
	mCallsPreviewList = (ListView) mCallsControlView.findViewById(R.id.lock_screen_call_preview_list);
	mCallsPreviewList.setItemsCanFocus(false);
	mCallsPreviewList.setVerticalScrollBarEnabled(false);
	mCallsPreviewList.setDrawingCacheEnabled(false);
	View hint = inflate(mContext,R.layout.lock_screen_call_hint,null);
	mCallsPreviewList.addHeaderView(hint);
	//mCallsAdapter = new CallLogPreviewListAdapter(mContext,null);
	//mCallsHandler = new QueryHandler(mContext,mCallsAdapter);
	//mCallsPreviewList.setAdapter(mCallsAdapter);
    }

    private void setupMusicControl(){
	mMusicControl = inflate(mContext,R.layout.lock_screen_music_control,null);
	mMusicControl.setLayoutParams(new FrameLayout.LayoutParams(-1,-2,0x50));
	mMusicControl.setDrawingCacheEnabled(false);
	mMusicTitle = (TextView) mMusicControl.findViewById(R.id.title);
	//mLockScreenAlbumController = new LockScreenAlbumController(mMusicControl);
	mMusicControl.findViewById(R.id.musicControlPrevious).setOnTouchListener(this);
	mMusicControl.findViewById(R.id.musicControlNext).setOnTouchListener(this);
	mMusicPlayPauseButton = (ImageView) mMusicControl.findViewById(R.id.musicControlPlay);
	mMusicPlayPauseButton.setOnTouchListener(this);
	mMusicControl.setVisibility(View.GONE);
    }

    private void setupSlidingPanel(){
	mSelector = new SlidingPanel(mContext);
	addView(mSelector,new FrameLayout.LayoutParams(-1,-2,0x53));
	mSelector.setTimeView(mTimeView,null);
	mSelector.setOnTriggerListener(this);
	mControlView = mSelector.getControlView();
	mControlView.setVisibility(View.INVISIBLE);
	LayoutParams layoutParams = (LayoutParams) mAlbumView.getLayoutParams();
	layoutParams.bottomMargin = mSelector.getBottomHeight();
	mAlbumView.setLayoutParams(layoutParams);
	layoutParams = (LayoutParams) mTempAlbumView.getLayoutParams();
	layoutParams.bottomMargin = mSelector.getBottomHeight();
	mTempAlbumView.setLayoutParams(layoutParams);
    }

    private void setupSmsPreviewList(){
	mSmsControlView = inflate(mContext,R.layout.lockscreen_sms_preview_list,null);
	mSmsPreviewList = (ListView) mSmsControlView.findViewById(R.id.lock_screen_sms_preview_list);
	mSmsPreviewList.setItemsCanFocus(false);
	mSmsPreviewList.setVerticalScrollBarEnabled(false);
	mSmsPreviewList.setDrawingCacheEnabled(true);
	View hint = inflate(mContext,R.layout.lock_screen_sms_hint,null);
	mSmsPreviewList.addHeaderView(hint);
	//mSmsAdapter = new SmsPreviewListAdapter(mContext);
	//mSmsHandler = new QueryHandler(mContext,mSmsAdapter);
	//mSmsPreviewList.setAdapter(mSmsAdapter);
    }

    private boolean shouldEnableMenuKey() {
        final Resources res = getResources();
        final boolean configDisabled = res.getBoolean(R.bool.config_disableMenuKeyInLockScreen);
        final boolean isMonkey = SystemProperties.getBoolean("ro.monkey", false);
        final boolean fileOverride = (new File(ENABLE_MENU_KEY_FILE)).exists();
        return !configDisabled || isMonkey || fileOverride;
    }

   /* private void startCallsQuery(){
	mCallsAdapter.setLoading(true);
	mCallsHandler.cancelOperation(0x35);
	StringBuilder where = new StringBuilder(" type=");
	where.append(3);
	where.append(" AND new=1 ");
	mCallsHandler.startQuery(0x35,null,Calls.CONTENT_URI,CallLog_COLUMNS,where.toString(),"0","date DESC");
    }

    private void startSmsQuery(){
	mSmsAdapter.setLoading(true);
	mSmsHandler.cancelOperation(0x35);
	mSmsHandler.startQuery(0x35,null,MmsSms.CONTENT_PREVIEW_URI,null,null,null,null);
    }*/

    private void toggleMusicControl(){
	boolean isMusicShowNow;
	if(mMusicControl.getVisibility() == View.VISIBLE){
	    isMusicShowNow = true;
	}else{
	    isMusicShowNow = false;
	}

	boolean isMusicActive = AudioSystem.isStreamActive(3);
	if(!isMusicShowNow){
	    mControlViewType = 4;
	    if(isMusicActive){
	      mMusicStatus = 2;
	    }else{
	      mMusicStatus = 1;
	    }
	}else{
	    mMusicStatus = 0;
	    mControlViewType = 0;
	    //mLockScreenAlbumController.enableAlbum(false);
	    //mLockScreenAlbumController.startAlbumAnim(2);
	    enableMusicControl(false);
	}
    }

    private void updateBackground(){
	int backgroundStatus = 1;
	boolean isMusicControlVisible;
	if(mMusicControl.getVisibility() == View.VISIBLE){
	    isMusicControlVisible = true;
	}else{
	    isMusicControlVisible = false;
	}

	/*if(isMusicControlVisible){
	    if(mLockScreenAlbumController.AlbumShow()){
	      backgroundStatus = 2;
	    }else{
	      backgroundStatus = 3;
	    }
	}*/

	switch(backgroundStatus){
	    case BACKGROUND_NONE:
		mBackgroundMask.setVisibility(View.GONE);
	    case BACKGROUND_MUSIC_SHOW_HAS_ALBUM: BACKGROUND_MUSIC_SHOW_NONE: BACKGROUND_MUSIC_SHOW_NO_ALBUM:
		if(mShowingBatteryInfo){
		    mBackgroundMask.setVisibility(View.VISIBLE);
		}else{
		    mBackgroundMask.setVisibility(View.GONE);
		}
	}
    }

    private void updateControlView(){
	if(mControlViewType == 4 && mMusicStatus != 0){
	    mControlViewType = 0;
	}else if(mControlViewType == 0 && mMusicStatus != 1){
	    if(mMusicStatus == 2){
		  //mLockScreenAlbumController.showAlbum();
	    }
	    updateBackground();
	}
	switch(mControlViewType){
	  case CONTROL_VIEW_MUSIC:
	      setControlView(mMusicControl,null);
	      updateMusic();
	  case CONTROL_VIEW_SMS:
	      //callAndSmsHandle(mSmsPreviewList,mSmsAdapter,mSmsControlView);
	  case CONTROL_VIEW_CALL:
	      //callAndSmsHandle(mCallsPreviewList,mCallsAdapter,mCallsControlView);
	  case CONTROL_VIEW_DATE:
	      setControlView(mHintView,null);
	  case CONTROL_VIEW_NONE:
	      break;
	}
    }

    private void updateLayout(Status status) {
        // The emergency call button no longer appears on this screen.
        if (DBG) Log.d(TAG, "updateLayout: status=" + status);
	
        

        switch (status) {
            case Normal:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                mUpdateMonitor.getTelephonySpn()));

                // Empty now, but used for sliding tab feedback
                mScreenLocked.setText("");

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                mSelector.setVisibility(View.VISIBLE);
                mEmergencyCallText.setVisibility(View.GONE);
		mEmergencyCall.setVisibility(View.GONE);
                break;
            case NetworkLocked:
                // The carrier string shows both sim card status (i.e. No Sim Card) and
                // carrier's name and/or "Emergency Calls Only" status
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_network_locked_message)));
                mScreenLocked.setText(R.string.lockscreen_instructions_when_pattern_disabled);

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                mSelector.setVisibility(View.VISIBLE);
                mEmergencyCallText.setVisibility(View.GONE);
		mEmergencyCall.setVisibility(View.GONE);
                break;
            case SimMissing:
                // text
                mCarrier.setText(R.string.lockscreen_missing_sim_message_short);
                mScreenLocked.setText(R.string.lockscreen_missing_sim_instructions);

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                mSelector.setVisibility(View.VISIBLE);
                mEmergencyCallText.setVisibility(View.VISIBLE);
		mEmergencyCall.setVisibility(View.VISIBLE);
                // do not need to show the e-call button; user may unlock
                break;
            case SimMissingLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_missing_sim_message_short)));
                mScreenLocked.setText(R.string.lockscreen_missing_sim_instructions);

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                mSelector.setVisibility(View.GONE);
                mEmergencyCallText.setVisibility(View.VISIBLE);
		mEmergencyCall.setVisibility(View.VISIBLE);
                break;
            case SimLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_sim_locked_message)));

                // layout
                mScreenLocked.setVisibility(View.INVISIBLE);
                mSelector.setVisibility(View.VISIBLE);
                mEmergencyCallText.setVisibility(View.GONE);
		mEmergencyCall.setVisibility(View.GONE);
                break;
            case SimPukLocked:
                // text
                mCarrier.setText(
                        getCarrierString(
                                mUpdateMonitor.getTelephonyPlmn(),
                                getContext().getText(R.string.lockscreen_sim_puk_locked_message)));
                mScreenLocked.setText(R.string.lockscreen_sim_puk_locked_instructions);

                // layout
                mScreenLocked.setVisibility(View.VISIBLE);
                mSelector.setVisibility(View.GONE);
                mEmergencyCallText.setVisibility(View.VISIBLE);
		mEmergencyCall.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateMusic(){
	boolean isMusicActive = AudioSystem.isStreamActive(3);
	boolean isShowPlaying = isMusicActive;
	mIsOnlineSongBlocking = false;
	isShowPlaying = false;

	if(isMusicActive){
	    mMusicPlayPauseButton.setImageResource(R.drawable.lock_screen_music_pause);
	}else{
	    mMusicPlayPauseButton.setImageResource(R.drawable.lock_screen_music_play);
	}

	switch(mMusicStatus){
	  case MUSIC_SHOW_STOP:
	      mMusicStatus = 1;
	  case MUSIC_SHOW_PLAY:
	      if(!isShowPlaying){
		mMusicStatus = 2;
	      }
	  case MUSIC_SHOW_NONE:
	      break;
	}
    }

    public void cleanUp(){
	mUpdateMonitor.removeCallback(this);
	mLockPatternUtils = null;
	mUpdateMonitor = null;
	//mCallsHandler.closeCursor();
	//mSmsHandler.closeCursor();
	mLockscreenWallpaperUpdater.cleanUp();
	mLockscreenInfo.cleanUp();
	mMusicWidget.cleanUp();
	mMissedEvent.cleanUp();
    }

    public boolean needsInput(){
      return false;
    }

    protected void onAttachedToWindow(){
	super.onAttachedToWindow();
	updateConfiguration();
	IntentFilter filter = new IntentFilter();
	filter.addAction("com.miui.player.metachanged");
	filter.addAction("lockscreen.action.SONG_METADATA_UPDATED");
	filter.addAction("com.miui.player.refreshprogress");
	mContext.registerReceiver(mPlayerStatusListener,filter,null,mHandler);
	//startCallsQuery();
	//startSmsQuery();
	//mContext.getContentResolver().registerContentObserver(Calls.CONTENT_URI,true,mCallsObserver);
	//mContext.getContentResolver().registerContentObserver(MmsSms.CONTENT_CONVERSATIONS_URI,true,mSmsObserver);
    }

    protected void onConfigurationChanged(Configuration newConfig){
	super.onConfigurationChanged(newConfig);
	updateConfiguration();
    }

    protected void onDetachedFromWindow(){
	//mContext.unregisterReceiver(mPlayerStatusListener);
	//mContext.getContentResolver().unregisterContentObserver(mCallsObserver);
	super.onDetachedFromWindow();
	//mContext.getContentResolver().unregisterContentObserver(mSmsContentObserver);
	super.onDetachedFromWindow();
    }

    public void onGrabbedStateChange(View v, int grabbedState){
	if(v == mSelector){
	    if(mSgsMusicControls){
		if(am.isMusicActive() || mAlwaysSgsMusicControls)
		  mMusicWidget.setVisibility(View.VISIBLE);
		  mMusicWidget.setControllerVisibility(true,mMusicWidget.isControllerShowing());	
		}else if(!am.isMusicActive()){
		  mMusicWidget.setVisibility(View.GONE);
		}
	    if(!isPaused || mPowerManager.isScreenOn()){
		
	    	switch(grabbedState){
		    case LEFT_HANDLE:
		      mControlViewType = 1;
		      removeCallbacks(mCheckStreamMusicRunnable);
		      updateControlView();
		    case RIGHT_HANDLE:
		      mControlViewType = 2;
		      removeCallbacks(mCheckStreamMusicRunnable);
		      updateControlView();
		    case DOUBLE_CLICK_HANDLE:
		      toggleMusicControl();
		    case MIDDLE_HANDLE:
		      mControlViewType = 3;
		      removeCallbacks(mCheckStreamMusicRunnable);
		      updateControlView();		      
		    case ANIMATION_CLICK_HANDLE:
		      removeCallbacks(mCheckStreamMusicRunnable);
		      mControlView.setVisibility(View.INVISIBLE);
		      mControlViewType = 0;
		      mControlView.clearAnimation();
		      mControlView.startAnimation(mFadeoutAnim);
		    case POKE_HANDLE:
		     // noHandle();
		      removeCallbacks(mCheckStreamMusicRunnable);
		      postDelayed(mCheckStreamMusicRunnable,0x3e8L);
		      mControlViewType = 4;
		      mControlView.setVisibility(View.VISIBLE);
		      updateControlView();
		    case SLIDING_HANDLE:
		      mControlView.setVisibility(View.INVISIBLE);
		      mSelector.setVisibility(View.INVISIBLE);
		    case NO_HANDLE:
		      break;
		}
	    }else{
	      mCallback.pokeWakelock();
	    }
	}
    }

    public boolean onKeyDown(int keyCode, KeyEvent event){
	if(keyCode == 0x52 || mEnableMenuKeyInLockScreen){
	    mCallback.goToUnlockScreen();
	}else{
	    return false;
	}
	return true;
    }

    public void onPause(){
	//mLockScreenAlbumController.enableAlbum(false);
	removeCallbacks(mCheckStreamMusicRunnable);
	isPaused = true;
	mSelector.onPause();
      if(mSgsMusicControls){
	mMusicWidget.onPause();
      }
      mMissedEvent.onPause();
    }

    public void onPhoneStateChanged(String newState){
	if(!TelephonyManager.EXTRA_STATE_IDLE.equals(newState) || !isPaused){
	    mSelector.clearBatteryAnimations();
	}else if(TelephonyManager.EXTRA_STATE_IDLE.equals(newState) || !isPaused){
	    refreshBatteryStringAndIcon();
	}else{
	    mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyCallText);
	}
    }

    public void onRefreshBatteryInfo(boolean showBatteryInfo, boolean pluggedIn, int batteryLevel){
	mShowingBatteryInfo = showBatteryInfo;
	mPluggedIn = pluggedIn;
	mBatteryLevel = batteryLevel;
	refreshBatteryStringAndIcon();
    }

    public void onRefreshCarrierInfo(CharSequence plmn, CharSequence spn) {
        if (DBG) Log.d(TAG, "onRefreshCarrierInfo(" + plmn + ", " + spn + ")");
        updateLayout(mStatus);
    }

    public void onResume(){
	boolean isMusicActive = AudioSystem.isStreamActive(3);
	if(isMusicActive){
	    mControlViewType = 4;
	    mMusicStatus = 2;
	    enableMusicControl(true);
	    //mLockScreenAlbumController.enableAlbum(true);
	}else{
	    mControlViewType = 0;
	    mMusicStatus = 0;
	    enableMusicControl(false);
	    //mLockScreenAlbumController.enableAlbum(false);
	    //mLockScreenAlbumController.hideAlbum();
	}
	resetStatusInfo(mUpdateMonitor);
	mLockPatternUtils.updateEmergencyCallButtonState(mEmergencyCallText);
	mLockscreenWallpaperUpdater.onResume();
	mLockscreenInfo.onResume();
      if(mSgsMusicControls){
	mMusicWidget.onResume();
	if(am.isMusicActive() || mAlwaysSgsMusicControls){
	  mMusicWidget.setVisibility(View.VISIBLE);
	  mMusicWidget.setControllerVisibility(true,mMusicWidget.isControllerShowing());	
	}else if(!am.isMusicActive()){
	  mMusicWidget.setVisibility(View.GONE);
	}
      }
      mMissedEvent.onResume();
	postDelayed(mCheckStreamMusicRunnable,0x3e8L);
	updateControlView();
	mSelector.onResume();
	isPaused = false;
    }

    public void onRingerModeChanged(int state){
    }

    public void onSimStateChanged(IccCard.State simState) {
        if (DBG) Log.d(TAG, "onSimStateChanged(" + simState + ")");
        mStatus = getCurrentStatus(simState);
        updateLayout(mStatus);
    }

    public void onTimeChanged(){
    }

    public boolean onTouch(View v, MotionEvent event){
	//MusicWidget.setVisibility(View.INVISIBLE);
	int keyCode = 0x55;
	int action;
	boolean showPlayIcon = false;
	switch(v.getId()){
	    case MotionEvent.ACTION_MOVE:
		keyCode = 0x58;
	    case MotionEvent.ACTION_DOWN:
		keyCode = 0x57;
	    case MotionEvent.ACTION_UP:
		break;
		
	}
	action = event.getAction() + 0xff;
	if(action != 0){
	    sendMediaButtonBroadcast(1,keyCode);
	    v.setPressed(true);
	}else if(action != 1){
	    if(action != 3){
		return true;
	    }
	}else{
	    sendMediaButtonBroadcast(1,keyCode);
	    v.setPressed(true);
	    if(keyCode == 0x55 || mMusicStatus == 2){
		showPlayIcon = true;
	    }else{
		showPlayIcon = false;
	    }
	}
	if(mMusicPlayPauseButton != null){
	    mMusicPlayPauseButton.setImageResource(R.drawable.lock_screen_music_play);
	}else{
	    mMusicPlayPauseButton.setImageResource(R.drawable.lock_screen_music_pause);
	}
	if(showPlayIcon){
	    mMusicStatus = 1;
	}else{
	    mMusicStatus = 2;
	}
	removeCallbacks(mCheckStreamMusicRunnable);
	postDelayed(mCheckStreamMusicRunnable,0xbb8L);
	return true;
    }
	    
    public void onTrigger(View v, int whichHandle){
	if(mSelector == v){
	    if(whichHandle == 1){
		Intent dialIntent = new Intent("android.intent.action.VIEW");
		dialIntent.setType("vnd.android.cursor.dir/calls");
		dialIntent.addCategory("android.intent.category.DEFAULT");
		dialIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		mContext.startActivity(dialIntent);
		mCallback.goToUnlockScreen();
	    }else if(whichHandle == 3){
		mCallback.setPendingIntent(null);
		mCallback.goToUnlockScreen();
	    }else if(whichHandle == 2){
	      try {
		Intent i;
		i = Intent.parseUri(mCustomAppActivity, 0);
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
	
    void updateConfiguration() {
        Configuration newConfig = getResources().getConfiguration();
        if (newConfig.orientation != mCreationOrientation) {
            mCallback.recreateMe(newConfig);
        } else if (newConfig.hardKeyboardHidden != mKeyboardHidden) {
            mKeyboardHidden = newConfig.hardKeyboardHidden;
            final boolean isKeyboardOpen = mKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            if (mUpdateMonitor.isKeyguardBypassEnabled() && isKeyboardOpen) {
                mCallback.goToUnlockScreen();
            }
        }
    }

    private void refreshMusicStatus() {
        if ((mWasMusicActive || mIsMusicActive || mLockAlwaysMusic
            || (mAudioManager.isWiredHeadsetOn())
            || (mAudioManager.isBluetoothA2dpOn())) && (mLockMusicControls)) {
            if (am.isMusicActive()) {
                mPauseIcon.setVisibility(View.VISIBLE);
                mPlayIcon.setVisibility(View.GONE);
                mRewindIcon.setVisibility(View.VISIBLE);
                mForwardIcon.setVisibility(View.VISIBLE);
            } else {
                mPlayIcon.setVisibility(View.VISIBLE);
                mPauseIcon.setVisibility(View.GONE);
                mRewindIcon.setVisibility(View.GONE);
                mForwardIcon.setVisibility(View.GONE);
            }
        } else {
            mPlayIcon.setVisibility(View.GONE);
            mPauseIcon.setVisibility(View.GONE);
            mRewindIcon.setVisibility(View.GONE);
            mForwardIcon.setVisibility(View.GONE);
        }
    }

    private void sendMediaButtonEvent(int code) {
        long eventtime = SystemClock.uptimeMillis();

        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, code, 0);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        getContext().sendOrderedBroadcast(downIntent, null);

        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP, code, 0);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        getContext().sendOrderedBroadcast(upIntent, null);
    }
}
